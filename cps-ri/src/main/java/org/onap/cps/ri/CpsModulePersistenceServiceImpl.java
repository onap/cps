/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.ri;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import io.micrometer.core.annotation.Timed;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.onap.cps.api.exceptions.AlreadyDefinedException;
import org.onap.cps.api.exceptions.DuplicatedYangResourceException;
import org.onap.cps.api.exceptions.ModelValidationException;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.api.model.SchemaSet;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.SchemaSetEntity;
import org.onap.cps.ri.models.YangResourceEntity;
import org.onap.cps.ri.models.YangResourceModuleReference;
import org.onap.cps.ri.repository.DataspaceRepository;
import org.onap.cps.ri.repository.ModuleReferenceRepository;
import org.onap.cps.ri.repository.SchemaSetRepository;
import org.onap.cps.ri.repository.YangResourceRepository;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangModelDependencyInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CpsModulePersistenceServiceImpl implements CpsModulePersistenceService {

    private static final String YANG_RESOURCE_CHECKSUM_CONSTRAINT_NAME = "yang_resource_checksum_key";
    private static final String NO_MODULE_NAME_FILTER = null;
    private static final String NO_MODULE_REVISION = null;
    private static final Pattern CHECKSUM_EXCEPTION_PATTERN = Pattern.compile(".*\\(checksum\\)=\\((\\w+)\\).*");
    private static final Pattern RFC6020_RECOMMENDED_FILENAME_PATTERN = Pattern
            .compile("([\\w-]+)@(\\d{4}-\\d{2}-\\d{2})(?:\\.yang)?", Pattern.CASE_INSENSITIVE);

    private final YangResourceRepository yangResourceRepository;

    private final SchemaSetRepository schemaSetRepository;

    private final DataspaceRepository dataspaceRepository;

    private final ModuleReferenceRepository moduleReferenceRepository;

    @Override
    public Map<String, String> getYangSchemaResources(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        return schemaSetEntity.getYangResources().stream().collect(
            Collectors.toMap(YangResourceEntity::getFileName, YangResourceEntity::getContent));
    }

    @Override
    public Collection<ModuleReference> getYangResourceModuleReferences(final String dataspaceName) {
        final Set<YangResourceModuleReference> yangResourceModuleReferenceList =
            yangResourceRepository.findAllModuleReferencesByDataspace(dataspaceName);
        return yangResourceModuleReferenceList.stream().map(CpsModulePersistenceServiceImpl::toModuleReference)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<ModuleReference> getYangResourceModuleReferences(final String dataspaceName,
                                                                       final String anchorName) {
        final Set<YangResourceModuleReference> yangResourceModuleReferenceList =
                yangResourceRepository
                        .findAllModuleReferencesByDataspaceAndAnchor(dataspaceName, anchorName);
        return yangResourceModuleReferenceList.stream().map(CpsModulePersistenceServiceImpl::toModuleReference)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ModuleDefinition> getYangResourceDefinitions(final String dataspaceName,
                                                                   final String anchorName) {
        final Set<YangResourceEntity> yangResourceEntities =
                yangResourceRepository.findAllModuleDefinitionsByDataspaceAndAnchorAndModule(dataspaceName, anchorName,
                    NO_MODULE_NAME_FILTER, NO_MODULE_REVISION);
        return convertYangResourceEntityToModuleDefinition(yangResourceEntities);
    }

    @Override
    public Collection<ModuleDefinition> getYangResourceDefinitionsByAnchorAndModule(final String dataspaceName,
                                                                                    final String anchorName,
                                                                                    final String moduleName,
                                                                                    final String moduleRevision) {
        final Set<YangResourceEntity> yangResourceEntities =
            yangResourceRepository.findAllModuleDefinitionsByDataspaceAndAnchorAndModule(dataspaceName, anchorName,
                moduleName, moduleRevision);
        return convertYangResourceEntityToModuleDefinition(yangResourceEntities);
    }

    private List<ModuleDefinition> convertYangResourceEntityToModuleDefinition(final Set<YangResourceEntity>
                                                                                   yangResourceEntities) {
        final List<ModuleDefinition> resultModuleDefinitions = new ArrayList<>(yangResourceEntities.size());
        for (final YangResourceEntity yangResourceEntity: yangResourceEntities) {
            resultModuleDefinitions.add(toModuleDefinition(yangResourceEntity));
        }
        return resultModuleDefinitions;
    }

    @Override
    @Transactional
    // A retry is made to store the schema set if it fails because of duplicated yang resource exception that
    // can occur in case of specific concurrent requests.
    @Retryable(retryFor = DuplicatedYangResourceException.class, maxAttempts = 5, backoff =
        @Backoff(random = true, delay = 200, maxDelay = 2000, multiplier = 2))
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
        final Map<String, String> moduleReferenceNameToContentMap) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Set<YangResourceEntity> yangResourceEntities = synchronizeYangResources(moduleReferenceNameToContentMap);
        final SchemaSetEntity schemaSetEntity = new SchemaSetEntity();
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
    public Collection<SchemaSet> getSchemaSetsByDataspaceName(final String dataspaceName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final List<SchemaSetEntity> schemaSetEntities = schemaSetRepository.findByDataspace(dataspaceEntity);
        return schemaSetEntities.stream()
                .map(CpsModulePersistenceServiceImpl::toSchemaSet).collect(Collectors.toList());
    }

    @Override
    @Transactional
    // A retry is made to store the schema set if it fails because of duplicated yang resource exception that
    // can occur in case of specific concurrent requests.
    @Retryable(retryFor = DuplicatedYangResourceException.class, maxAttempts = 5, backoff =
        @Backoff(random = true, delay = 200, maxDelay = 2000, multiplier = 2))
    @Timed(value = "cps.module.persistence.schemaset.store",
        description = "Time taken to store a schemaset (list of module references")
    public void storeSchemaSetFromModules(final String dataspaceName, final String schemaSetName,
                                          final Map<String, String> newModuleNameToContentMap,
                                          final Collection<ModuleReference> allModuleReferences) {
        storeSchemaSet(dataspaceName, schemaSetName, newModuleNameToContentMap);
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
                schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        final List<Integer> allYangResourceIds =
            yangResourceRepository.getResourceIdsByModuleReferences(allModuleReferences);
        yangResourceRepository.insertSchemaSetIdYangResourceId(schemaSetEntity.getId(), allYangResourceIds);
    }

    @Override
    @Transactional
    public void deleteSchemaSet(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        schemaSetRepository.delete(schemaSetEntity);
    }

    @Override
    @Transactional
    public void deleteSchemaSets(final String dataspaceName, final Collection<String> schemaSetNames) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        schemaSetRepository.deleteByDataspaceAndNameIn(dataspaceEntity, schemaSetNames);
    }


    @Override
    @Transactional
    public void updateSchemaSetFromModules(final String dataspaceName, final String schemaSetName,
                                           final Map<String, String> newModuleNameToContentMap,
                                           final Collection<ModuleReference> allModuleReferences) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        storeAndLinkNewModules(newModuleNameToContentMap, schemaSetEntity);
        updateAllModuleReferences(allModuleReferences, schemaSetEntity.getId());
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

    @Override
    public Collection<ModuleReference> getModuleReferencesByAttribute(final String dataspaceName,
                                                                      final String anchorName,
                                                                      final Map<String, String> parentAttributes,
                                                                      final Map<String, String> childAttributes) {
        return moduleReferenceRepository.findModuleReferences(dataspaceName, anchorName, parentAttributes,
                childAttributes);
    }

    private Set<YangResourceEntity> synchronizeYangResources(
        final Map<String, String> moduleReferenceNameToContentMap) {
        final Map<String, YangResourceEntity> checksumToEntityMap = moduleReferenceNameToContentMap.entrySet().stream()
            .map(entry -> {
                final String checksum = DigestUtils.sha256Hex(entry.getValue().getBytes(StandardCharsets.UTF_8));
                final Map<String, String> moduleNameAndRevisionMap = createModuleNameAndRevisionMap(entry.getKey(),
                            entry.getValue());
                final YangResourceEntity yangResourceEntity = new YangResourceEntity();
                yangResourceEntity.setFileName(entry.getKey());
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
                        e -> {
                            final RetryContext retryContext = RetrySynchronizationManager.getContext();
                            int retryCount = retryContext == null ? 0 : retryContext.getRetryCount();
                            log.warn("Cannot persist duplicated yang resource. System will attempt this method "
                                    + "up to 5 times. Current retry count : {}", ++retryCount, e);
                        });
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
        final RevisionSourceIdentifier revisionSourceIdentifier =
            createIdentifierFromSourceName(checkNotNull(sourceName));

        final YangTextSchemaSource tempYangTextSchemaSource = new YangTextSchemaSource(revisionSourceIdentifier) {
            @Override
            public Optional<String> getSymbolicName() {
                return Optional.empty();
            }

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
            final YangModelDependencyInfo yangModelDependencyInfo
                = YangModelDependencyInfo.forYangText(tempYangTextSchemaSource);
            metaDataMap.put("moduleName", yangModelDependencyInfo.getName());
            metaDataMap.put("revision", yangModelDependencyInfo.getFormattedRevision());
        } catch (final YangSyntaxErrorException | IOException e) {
            throw new ModelValidationException("Yang resource is invalid.",
                   String.format("Yang syntax validation failed for resource %s:%n%s", sourceName, e.getMessage()), e);
        }
        return metaDataMap;
    }

    private static RevisionSourceIdentifier createIdentifierFromSourceName(final String sourceName) {
        final Matcher matcher = RFC6020_RECOMMENDED_FILENAME_PATTERN.matcher(sourceName);
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

    private String getNameForChecksum(final String checksum,
                                      final Collection<YangResourceEntity> yangResourceEntities) {
        final Optional<String> optionalFileName = yangResourceEntities.stream()
                        .filter(entity -> StringUtils.equals(checksum, (entity.getChecksum())))
                        .findFirst()
                        .map(YangResourceEntity::getFileName);
        return optionalFileName.orElse("no filename");
    }

    private String getDuplicatedChecksumFromException(final ConstraintViolationException exception) {
        final Matcher matcher = CHECKSUM_EXCEPTION_PATTERN.matcher(exception.getSQLException().getMessage());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "no checksum found";
    }

    private static ModuleReference toModuleReference(
        final YangResourceModuleReference yangResourceModuleReference) {
        return ModuleReference.builder()
            .moduleName(yangResourceModuleReference.getModuleName())
            .revision(yangResourceModuleReference.getRevision())
            .build();
    }

    private static ModuleDefinition toModuleDefinition(final YangResourceEntity yangResourceEntity) {
        return new ModuleDefinition(
                yangResourceEntity.getModuleName(),
                yangResourceEntity.getRevision(),
                yangResourceEntity.getContent());
    }

    private static SchemaSet toSchemaSet(final SchemaSetEntity schemaSetEntity) {
        return SchemaSet.builder().name(schemaSetEntity.getName())
                .dataspaceName(schemaSetEntity.getDataspace().getName()).build();
    }

    private void storeAndLinkNewModules(final Map<String, String> newModuleNameToContentMap,
                                        final SchemaSetEntity schemaSetEntity) {
        final Set<YangResourceEntity> yangResourceEntities
            = new HashSet<>(synchronizeYangResources(newModuleNameToContentMap));
        schemaSetEntity.setYangResources(yangResourceEntities);
        schemaSetRepository.save(schemaSetEntity);
    }

    private void updateAllModuleReferences(final Collection<ModuleReference> allModuleReferences,
                                           final Integer schemaSetEntityId) {
        yangResourceRepository.deleteSchemaSetYangResourceForSchemaSetId(schemaSetEntityId);
        final List<Integer> allYangResourceIds =
            yangResourceRepository.getResourceIdsByModuleReferences(allModuleReferences);
        yangResourceRepository.insertSchemaSetIdYangResourceId(schemaSetEntityId, allYangResourceIds);
    }

}
