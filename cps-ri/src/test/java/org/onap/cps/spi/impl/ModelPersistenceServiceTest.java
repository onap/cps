/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.SchemaSet;
import org.onap.cps.spi.entities.YangResource;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ModelPersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/schemaset.sql";

    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String DATASPACE_NAME_INVALID = "DATASPACE-X";
    private static final String SCHEMA_SET_NAME = "SCHEMA-SET-001";
    private static final String SCHEMA_SET_NAME_NEW = "SCHEMA-SET-NEW";
    private static final String OLD_RESOURCE_CONTENT = "CONTENT-001";
    private static final String OLD_RESOURCE_CHECKSUM = "877e65a9f36d54e7702c3f073f6bc42b";
    private static final String NEW_RESOURCE_CONTENT = "CONTENT-NEW";
    private static final String NEW_RESOURCE_CHECKSUM = "c94d40a1350eb1c0b1c1949eac84fc59";

    @ClassRule
    public static DatabaseTestContainer testContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsModulePersistenceService cpsModulePersistenceService;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private YangResourceRepository yangResourceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;


    @Test(expected = CpsValidationException.class)
    @Sql(CLEAR_DATA)
    public void testStoreSchemaSetToInvalidDataspace() {
        cpsModulePersistenceService
            .storeSchemaSet(DATASPACE_NAME_INVALID, SCHEMA_SET_NAME_NEW, toSet(NEW_RESOURCE_CONTENT));
    }

    @Test(expected = CpsValidationException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreDuplicateSchemaSet() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME, toSet(NEW_RESOURCE_CONTENT));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSetWithNewYangResource() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, toSet(NEW_RESOURCE_CONTENT));
        assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
            0L /* omit resource id value check */, NEW_RESOURCE_CONTENT, NEW_RESOURCE_CHECKSUM);
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSetWithExistingYangResourceReuse() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, toSet(OLD_RESOURCE_CONTENT));
        assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
            3001L, OLD_RESOURCE_CONTENT, OLD_RESOURCE_CHECKSUM);
    }

    private void assertSchemaSetPersisted(final String expectedDataspaceName, final String expectedSchemaSetName,
                                          final Long expectedYangResourceId, final String expectedYangResourceContent,
                                          final String expectedYangResourceChecksum) {

        // assert the schema set is persisted
        final SchemaSet schemaSet = getSchemaSetFromDatabase(expectedDataspaceName, expectedSchemaSetName);
        assertEquals(expectedDataspaceName, schemaSet.getDataspace().getName());
        assertEquals(expectedSchemaSetName, schemaSet.getName());

        // assert the attached yang resource is persisted
        final Set<YangResource> yangResources = schemaSet.getYangResources();
        assertNotNull(yangResources);
        assertEquals(1, yangResources.size());

        // assert the attached yang resource content
        final YangResource yangResource = yangResources.iterator().next();
        assertNotNull(yangResource.getId());
        if (expectedYangResourceId > 0L) {
            // existing resource with known id
            assertEquals(expectedYangResourceId, yangResource.getId());
        }
        assertEquals(expectedYangResourceContent, yangResource.getContent());
        assertEquals(expectedYangResourceChecksum, yangResource.getChecksum());
    }

    private static Set<String> toSet(final String... elements) {
        return ImmutableSet.<String>builder().add(elements).build();
    }

    private SchemaSet getSchemaSetFromDatabase(final String dataspaceName, final String schemaSetName) {
        final Dataspace dataspace = dataspaceRepository.findByName(dataspaceName).orElseThrow();
        return schemaSetRepository.findByDataspaceAndName(dataspace, schemaSetName).orElseThrow();
    }
}
