/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.collect.ImmutableSet;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.DuplicatedYangResourceException;
import org.onap.cps.spi.exceptions.SchemaSetInUseException;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class CpsModulePersistenceServiceImpl implements CpsModulePersistenceService {

    private static final String YANG_RESOURCE_CHECKSUM_CONSTRAINT_NAME = "yang_resource_checksum_key";
    private static final String CHECKUM_EXCPETION_REGEX = ".*\\(checksum\\)=\\((\\w+)\\).*";
    private static final Pattern CHECKUM_EXCPETION_PATTERN = Pattern.compile(CHECKUM_EXCPETION_REGEX);

    @Autowired
    private YangResourceRepository yangResourceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private AnchorRepository anchorRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Autowired
    private CpsAdminPersistenceService cpsAdminPersistenceService;

    @Override
    @Transactional
    // A retry is made to store the schema set if it fails because of duplicated yang resource exception that
    // can occur in case of specific concurrent requests.
    @Retryable(value = DuplicatedYangResourceException.class, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
        final Map<String, String> yangResourcesNameToContentMap) {

        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Set<YangResourceEntity> yangResourceEntities = synchronizeYangResources(yangResourcesNameToContentMap);
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

    private Set<YangResourceEntity> synchronizeYangResources(final Map<String, String> yangResourcesNameToContentMap) {
        final Map<String, YangResourceEntity> checksumToEntityMap = yangResourcesNameToContentMap.entrySet().stream()
            .map(entry -> {
                final String checksum = DigestUtils.sha256Hex(entry.getValue().getBytes(StandardCharsets.UTF_8));
                final var yangResourceEntity = new YangResourceEntity();
                yangResourceEntity.setName(entry.getKey());
                yangResourceEntity.setContent(entry.getValue());
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
                final var convertedException =
                        convertToDuplicatedYangResourceException(
                                dataIntegrityViolationException, newYangResourceEntities);
                if (convertedException != null) {
                    log.warn("Cannot persist duplicated yang resource. "
                                    + "A total of 2 attempts to store the schema set are planned.", convertedException);
                }
                throw convertedException != null ? convertedException : dataIntegrityViolationException;
            }
        }

        return ImmutableSet.<YangResourceEntity>builder()
            .addAll(existingYangResourceEntities)
            .addAll(newYangResourceEntities)
            .build();
    }

    /**
     * Convert the specified data integrity violation exception into a CPS duplicated Yang resource exception
     * if the cause of the error is a yang checksum database constraint violation.
     * @param originalException the original db exception.
     * @param yangResourceEntities the collection of Yang resources involved in the db failure.
     * @return the converted CPS duplicated Yang resource exception or null if the original cause of the error is not
     *     a yang checksum database constraint violation.
     */
    private DuplicatedYangResourceException convertToDuplicatedYangResourceException(
            final DataIntegrityViolationException originalException,
            final Collection<YangResourceEntity> yangResourceEntities) {

        // The exception result
        DuplicatedYangResourceException duplicatedYangResourceException = null;

        final Throwable cause = originalException.getCause();
        if (cause instanceof ConstraintViolationException) {
            final ConstraintViolationException constraintException = (ConstraintViolationException) cause;
            if (constraintException.getConstraintName() != null
                        && constraintException.getConstraintName().equals(
                                YANG_RESOURCE_CHECKSUM_CONSTRAINT_NAME)) {
                // Db constraint related to yang resource checksum uniqueness is not respected
                final String checksumInError = getDuplicatedChecksumFromException(constraintException);
                final String nameInError = getNameForChecksum(checksumInError, yangResourceEntities);
                duplicatedYangResourceException =
                        new DuplicatedYangResourceException(nameInError, checksumInError, constraintException);
            }
        }

        return duplicatedYangResourceException;

    }

    /**
     * Get the checksum that caused the constraint violation exception.
     * @param exception the exception having the checksum in error.
     * @return the checksum in error or null if not found.
     */
    private String getDuplicatedChecksumFromException(final ConstraintViolationException exception) {
        String checksum = null;
        final Matcher matcher = CHECKUM_EXCPETION_PATTERN.matcher(exception.getSQLException().getMessage());
        if (matcher.find() && matcher.groupCount() == 1) {
            checksum = matcher.group(1);
        }
        return checksum;
    }

    /**
     * Get the name of the yang resource having the specified checksum.
     * @param checksum the checksum.
     * @param yangResourceEntities the list of yang resources to search among.
     * @return the name found or null if none.
     */
    private String getNameForChecksum(
            final String checksum, final Collection<YangResourceEntity> yangResourceEntities) {
        String name = null;
        if (checksum != null) {
            for (final YangResourceEntity yangResourceEntity : yangResourceEntities) {
                if (checksum.equals(yangResourceEntity.getChecksum())) {
                    name = yangResourceEntity.getName();
                }
            }
        }
        return name;
    }

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
    @Transactional
    public void deleteSchemaSet(final String dataspaceName, final String schemaSetName,
        final CascadeDeleteAllowed cascadeDeleteAllowed) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);

        final Collection<AnchorEntity> anchorEntities = anchorRepository.findAllBySchemaSet(schemaSetEntity);
        if (!anchorEntities.isEmpty()) {
            if (cascadeDeleteAllowed != CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED) {
                throw new SchemaSetInUseException(dataspaceName, schemaSetName);
            }
            fragmentRepository.deleteByAnchorIn(anchorEntities);
            anchorRepository.deleteAll(anchorEntities);
        }
        schemaSetRepository.delete(schemaSetEntity);
        yangResourceRepository.deleteOrphans();
    }
}
