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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.ModelPersistenceService;
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
    private static final String OLD_FILE_CONTENT = "CONTENT-001";
    private static final String NEW_FILE_CONTENT = "CONTENT-NEW";
    private static final String NEW_FILE_CHECKSUM = "c94d40a1350eb1c0b1c1949eac84fc59";

    @ClassRule
    public static DatabaseTestContainer testContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private ModelPersistenceService modelPersistenceService;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private YangResourceRepository yangResourceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;


    @Test(expected = CpsValidationException.class)
    @Sql(CLEAR_DATA)
    public void testStoreSchemaSetToInvalidDataspace() {
        modelPersistenceService.storeSchemaSet(DATASPACE_NAME_INVALID, SCHEMA_SET_NAME_NEW, toSet(NEW_FILE_CONTENT));
    }

    @Test(expected = CpsValidationException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreDuplicateSchemaSet() {
        modelPersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME, toSet(NEW_FILE_CONTENT));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSet() {
        modelPersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, toSet(NEW_FILE_CONTENT));

        // validate the schema set is persisted
        final SchemaSet schemaSet = getSchemaSetFromDatabase(DATASPACE_NAME, SCHEMA_SET_NAME_NEW);
        assertEquals(DATASPACE_NAME, schemaSet.getDataspace().getName());
        assertEquals(SCHEMA_SET_NAME_NEW, schemaSet.getName());

        // validate the attached resource persisted as well
        final Set<YangResource> yangResourceEntities = schemaSet.getYangResources();
        assertNotNull(yangResourceEntities);
        assertEquals(1, yangResourceEntities.size());

        final YangResource yangResource = yangResourceEntities.iterator().next();
        assertNotNull(yangResource.getId());
        assertEquals(NEW_FILE_CONTENT, yangResource.getContent());
        assertEquals(NEW_FILE_CHECKSUM, yangResource.getChecksum());
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSetReuseFiles() {

        modelPersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, toSet(OLD_FILE_CONTENT));

        // validate the schema set is persisted
        final SchemaSet schemaSet = getSchemaSetFromDatabase(DATASPACE_NAME, SCHEMA_SET_NAME_NEW);
        assertEquals(DATASPACE_NAME, schemaSet.getDataspace().getName());
        assertEquals(SCHEMA_SET_NAME_NEW, schemaSet.getName());

        // validate the attached resource is reused ()
        final Set<YangResource> yangResources = schemaSet.getYangResources();
        assertNotNull(yangResources);
        assertEquals(1, yangResources.size());

        final YangResource yangResource = yangResources.iterator().next();
        assertEquals(Long.valueOf(3001), yangResource.getId());
        assertEquals(OLD_FILE_CONTENT, yangResource.getContent());
        assertEquals("877e65a9f36d54e7702c3f073f6bc42b", yangResource.getChecksum());
    }

    private static Set<String> toSet(final String... entries) {
        return new HashSet<>(Arrays.asList(entries));
    }

    private SchemaSet getSchemaSetFromDatabase(final String dataspaceName, final String moduleSetName) {
        final Dataspace dataspace = dataspaceRepository.findByName(dataspaceName).orElseThrow();
        return schemaSetRepository.findByDataspaceAndName(dataspace, moduleSetName).orElseThrow();
    }
}
