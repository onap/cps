/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.entities.YangResourceModuleReference;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.DuplicatedYangResourceException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.ModuleReferenceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangModelDependencyInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class CpsModulePersistenceServiceImpl implements CpsModulePersistenceService {

    private static final String YANG_RESOURCE_CHECKSUM_CONSTRAINT_NAME = "yang_resource_checksum_key";
    private static final Pattern CHECKSUM_EXCEPTION_PATTERN = Pattern.compile(".*\\(checksum\\)=\\((\\w+)\\).*");
    private static final Pattern RFC6020_RECOMMENDED_FILENAME_PATTERN = Pattern
            .compile("([\\w-]+)@(\\d{4}-\\d{2}-\\d{2})(?:\\.yang)?", Pattern.CASE_INSENSITIVE);

    private YangResourceRepository yangResourceRepository;

    private SchemaSetRepository schemaSetRepository;

    private DataspaceRepository dataspaceRepository;

    private CpsAdminPersistenceService cpsAdminPersistenceService;

    private ModuleReferenceRepository moduleReferenceRepository;

    @Override
    public Map<String, String> getYangSchemaResources(final String dataspaceName, final String schemaSetName) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        return schemaSetEntity.getYangResources().stream().collect(
            Collectors.toMap(YangResourceEntity::getName, YangResourceEntity::getContent));
    }

    @Override
    public Map<String, String> getYangSchemaSetResources(final String dataspaceName, final String anchorName) {
        final var anchor = cpsAdminPersistenceService.getAnchor(dataspaceName, anchorName);
        return getYangSchemaResources(dataspaceName, anchor.getSchemaSetName());
    }

    @Override
    public Collection<ModuleReference> getYangResourceModuleReferences(final String dataspaceName) {
        final Set<YangResourceModuleReference> yangResourceModuleReferenceList =
            yangResourceRepository.findAllModuleReferences(dataspaceName);
        return yangResourceModuleReferenceList.stream().map(CpsModulePersistenceServiceImpl::toModuleReference)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<ModuleReference> getYangResourceModuleReferences(final String dataspaceName,
        final String anchorName) {
        final Set<YangResourceModuleReference> yangResourceModuleReferenceList =
            yangResourceRepository
                .findAllModuleReferences(dataspaceName, anchorName);
        return yangResourceModuleReferenceList.stream().map(CpsModulePersistenceServiceImpl::toModuleReference)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    // A retry is made to store the schema set if it fails because of duplicated yang resource exception that
    // can occur in case of specific concurrent requests.
    @Retryable(value = DuplicatedYangResourceException.class, maxAttempts = 5, backoff =
        @Backoff(random = true, delay = 200, maxDelay = 2000, multiplier = 2))
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
        final Map<String, String> moduleReferenceNameToContentMap) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var yangResourceEntities = synchronizeYangResources(moduleReferenceNameToContentMap);
        final var schemaSetEntity = new SchemaSetEntity();
        schemaSetEntity.setName(schemaSetName);
        schemaSetEntity.setDataspace(dataspaceEntity);
        schemaSetEntity.setYangResources(yangResourceEntities);
        try {
            schemaSetRepository.save(schemaSetEntity);
        } catch (final DataIntegrityViolationException e) {
            throw AlreadyDefinedException.forSchemaSet(schemaSetName, dataspaceName, e);
        }
    }

    @Override
    @Transactional
    // A retry is made to store the schema set if it fails because of duplicated yang resource exception that
    // can occur in case of specific concurrent requests.
    @Retryable(value = DuplicatedYangResourceException.class, maxAttempts = 5, backoff =
        @Backoff(random = true, delay = 200, maxDelay = 2000, multiplier = 2))
    public void storeSchemaSetFromModules(final String dataspaceName, final String schemaSetName,
                                          final Map<String, String> newModuleNameToContentMap,
                                          final Collection<ModuleReference> moduleReferences) {
        storeSchemaSet(dataspaceName, schemaSetName, newModuleNameToContentMap);
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var schemaSetEntity =
                schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        final List<Long> listOfYangResourceIds = new ArrayList<>();
        moduleReferences.forEach(moduleReference ->
                listOfYangResourceIds.add(yangResourceRepository.getIdByModuleNameAndRevision(
                        moduleReference.getModuleName(), moduleReference.getRevision())));
        yangResourceRepository.insertSchemaSetIdYangResourceId(schemaSetEntity.getId(), listOfYangResourceIds);
    }

    @Override
    @Transactional
    public void deleteSchemaSet(final String dataspaceName, final String schemaSetName) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        schemaSetRepository.delete(schemaSetEntity);
    }

    @Override
    @Transactional
    public void deleteUnusedYangResourceModules() {
        yangResourceRepository.deleteOrphans();
    }

    @Override
    public Collection<ModuleReference> identifyNewModuleReferences(
        final Collection<ModuleReference> moduleReferencesToCheck) {
        return moduleReferenceRepository.identifyNewModuleReferences(moduleReferencesToCheck);
    }

    private Set<YangResourceEntity> synchronizeYangResources(
        final Map<String, String> moduleReferenceNameToContentMap) {
        final Map<String, YangResourceEntity> checksumToEntityMap = moduleReferenceNameToContentMap.entrySet().stream()
            .map(entry -> {
                final String checksum = DigestUtils.sha256Hex(entry.getValue().getBytes(StandardCharsets.UTF_8));
                final Map<String, String> moduleNameAndRevisionMap = createModuleNameAndRevisionMap(entry.getKey(),
                            entry.getValue());
                final var yangResourceEntity = new YangResourceEntity();
                yangResourceEntity.setName(entry.getKey());
                yangResourceEntity.setContent(entry.getValue());
                yangResourceEntity.setModuleName(moduleNameAndRevisionMap.get("moduleName"));
                yangResourceEntity.setRevision(moduleNameAndRevisionMap.get("revision"));
                yangResourceEntity.setChecksum(checksum);
                return yangResourceEntity;
            })
            .collect(Collectors.toMap(
                YangResourceEntity::getChecksum,
                entity -> entity
            ));

        final List<YangResourceEntity> existingYangResourceEntities =
            yangResourceRepository.findAllByChecksumIn(checksumToEntityMap.keySet());
        existingYangResourceEntities.forEach(yangFile -> checksumToEntityMap.remove(yangFile.getChecksum()));

        final Collection<YangResourceEntity> newYangResourceEntities = checksumToEntityMap.values();
        if (!newYangResourceEntities.isEmpty()) {
            try {
                yangResourceRepository.saveAll(newYangResourceEntities);
            } catch (final DataIntegrityViolationException dataIntegrityViolationException) {
                // Throw a CPS duplicated Yang resource exception if the cause of the error is a yang checksum
                // database constraint violation.
                // If it is not, then throw the original exception
                final Optional<DuplicatedYangResourceException> convertedException =
                        convertToDuplicatedYangResourceException(
                                dataIntegrityViolationException, newYangResourceEntities);
                convertedException.ifPresent(
                    e ->  log.warn(
                                "Cannot persist duplicated yang resource. "
                                        + "System will attempt this method up to 5 times.", e));
                throw convertedException.isPresent() ? convertedException.get() : dataIntegrityViolationException;
            }
        }

        return ImmutableSet.<YangResourceEntity>builder()
            .addAll(existingYangResourceEntities)
            .addAll(newYangResourceEntities)
            .build();
    }

    private static Map<String, String> createModuleNameAndRevisionMap(final String sourceName, final String source) {
        final Map<String, String> metaDataMap = new HashMap<>();
        final var revisionSourceIdentifier =
                createIdentifierFromSourceName(checkNotNull(sourceName));

        final var tempYangTextSchemaSource = new YangTextSchemaSource(revisionSourceIdentifier) {
            @Override
            protected MoreObjects.ToStringHelper addToStringAttributes(
                    final MoreObjects.ToStringHelper toStringHelper) {
                return toStringHelper;
            }

            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
            }
        };
        try {
            final var dependencyInfo = YangModelDependencyInfo.forYangText(tempYangTextSchemaSource);
            metaDataMap.put("moduleName", dependencyInfo.getName());
            metaDataMap.put("revision", dependencyInfo.getFormattedRevision());
        } catch (final YangSyntaxErrorException | IOException e) {
            throw new ModelValidationException("Yang resource is invalid.",
                   String.format("Yang syntax validation failed for resource %s:%n%s", sourceName, e.getMessage()), e);
        }
        return metaDataMap;
    }

    private static RevisionSourceIdentifier createIdentifierFromSourceName(final String sourceName) {
        final var matcher = RFC6020_RECOMMENDED_FILENAME_PATTERN.matcher(sourceName);
        if (matcher.matches()) {
            return RevisionSourceIdentifier.create(matcher.group(1), Revision.of(matcher.group(2)));
        }
        return RevisionSourceIdentifier.create(sourceName);
    }

    /**
     * Convert the specified data integrity violation exception into a CPS duplicated Yang resource exception
     * if the cause of the error is a yang checksum database constraint violation.
     *
     * @param originalException the original db exception.
     * @param yangResourceEntities the collection of Yang resources involved in the db failure.
     * @return an optional converted CPS duplicated Yang resource exception. The optional is empty if the original
     *      cause of the error is not a yang checksum database constraint violation.
     */
    private Optional<DuplicatedYangResourceException> convertToDuplicatedYangResourceException(
            final DataIntegrityViolationException originalException,
            final Collection<YangResourceEntity> yangResourceEntities) {

        // The exception result
        DuplicatedYangResourceException duplicatedYangResourceException = null;

        final Throwable cause = originalException.getCause();
        if (cause instanceof ConstraintViolationException) {
            final ConstraintViolationException constraintException = (ConstraintViolationException) cause;
            if (YANG_RESOURCE_CHECKSUM_CONSTRAINT_NAME.equals(constraintException.getConstraintName())) {
                // Db constraint related to yang resource checksum uniqueness is not respected
                final String checksumInError = getDuplicatedChecksumFromException(constraintException);
                final String nameInError = getNameForChecksum(checksumInError, yangResourceEntities);
                duplicatedYangResourceException =
                        new DuplicatedYangResourceException(nameInError, checksumInError, constraintException);
            }
        }

        return Optional.ofNullable(duplicatedYangResourceException);

    }

    /**
     * Get the name of the yang resource having the specified checksum.
     *
     * @param checksum the checksum. Null is supported.
     * @param yangResourceEntities the list of yang resources to search among.
     * @return the name found or null if none.
     */
    private String getNameForChecksum(
            final String checksum, final Collection<YangResourceEntity> yangResourceEntities) {
        return
                yangResourceEntities.stream()
                        .filter(entity -> StringUtils.equals(checksum, (entity.getChecksum())))
                        .findFirst()
                        .map(YangResourceEntity::getName)
                        .orElse(null);
    }

    /**
     * Get the checksum that caused the constraint violation exception.
     *
     * @param exception the exception having the checksum in error.
     * @return the checksum in error or null if not found.
     */
    private String getDuplicatedChecksumFromException(final ConstraintViolationException exception) {
        String checksum = null;
        final var matcher = CHECKSUM_EXCEPTION_PATTERN.matcher(exception.getSQLException().getMessage());
        if (matcher.find() && matcher.groupCount() == 1) {
            checksum = matcher.group(1);
        }
        return checksum;
    }

    private static ModuleReference toModuleReference(
        final YangResourceModuleReference yangResourceModuleReference) {
        return ModuleReference.builder()
            .moduleName(yangResourceModuleReference.getModuleName())
            .revision(yangResourceModuleReference.getRevision())
            .build();
    }
}
