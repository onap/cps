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

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.exceptions.AnchorNotFoundException;
import org.onap.cps.spi.exceptions.DataspaceNotFoundException;
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CpsModulePersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/schemaset.sql";
    private static final String SET_DATA_ANCHOR = "/data/anchor.sql";

    private static final String ANCHOR_NAME1 = "ANCHOR-001";
    private static final String NON_EXISTING_ANCHOR_NAME = "NON EXISTING ANCHOR NAME";
    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String DATASPACE_NAME_INVALID = "DATASPACE-X";
    private static final String SCHEMA_SET_NAME = "SCHEMA-SET-001";
    private static final String SCHEMA_SET_NAME_NEW = "SCHEMA-SET-NEW";

    private static final String EXISTING_RESOURCE_NAME = "module1@2020-02-02.yang";
    private static final String EXISTING_RESOURCE_CONTENT = "CONTENT-001";
    private static final String EXISTING_RESOURCE_CHECKSUM = "877e65a9f36d54e7702c3f073f6bc42b";
    private static final Long EXISTING_RESOURCE_ID = 3001L;

    private static final String NEW_RESOURCE_NAME = "new-module@2020-02-02.yang";
    private static final String NEW_RESOURCE_CONTENT = "CONTENT-NEW";
    private static final String NEW_RESOURCE_CHECKSUM = "c94d40a1350eb1c0b1c1949eac84fc59";
    private static final Long NEW_RESOURCE_ABSTRACT_ID = 0L;

    @ClassRule
    public static DatabaseTestContainer testContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsModulePersistenceService cpsModulePersistenceService;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;


    @Test(expected = DataspaceNotFoundException.class)
    @Sql(CLEAR_DATA)
    public void testStoreSchemaSetToInvalidDataspace() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME_INVALID, SCHEMA_SET_NAME_NEW,
            toMap(NEW_RESOURCE_NAME, NEW_RESOURCE_CONTENT));
    }

    @Test(expected = SchemaSetAlreadyDefinedException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreDuplicateSchemaSet() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME,
            toMap(NEW_RESOURCE_NAME, NEW_RESOURCE_CONTENT));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSetWithNewYangResource() {
        final Map<String, String> yangResourcesNameToContentMap = toMap(NEW_RESOURCE_NAME, NEW_RESOURCE_CONTENT);
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
            yangResourcesNameToContentMap);
        assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
            NEW_RESOURCE_ABSTRACT_ID, NEW_RESOURCE_NAME, NEW_RESOURCE_CONTENT, NEW_RESOURCE_CHECKSUM);
        assertEquals(yangResourcesNameToContentMap,
                cpsModulePersistenceService.getYangSchemaResources(DATASPACE_NAME, SCHEMA_SET_NAME_NEW));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSetWithExistingYangResourceReuse() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
            toMap(NEW_RESOURCE_NAME, EXISTING_RESOURCE_CONTENT));
        assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
            EXISTING_RESOURCE_ID, EXISTING_RESOURCE_NAME, EXISTING_RESOURCE_CONTENT, EXISTING_RESOURCE_CHECKSUM);
    }

    private void assertSchemaSetPersisted(final String expectedDataspaceName, final String expectedSchemaSetName,
                                          final Long expectedYangResourceId, final String expectedYangResourceName,
                                          final String expectedYangResourceContent,
                                          final String expectedYangResourceChecksum) {

        // assert the schema set is persisted
        final SchemaSetEntity schemaSetEntity = getSchemaSetFromDatabase(expectedDataspaceName, expectedSchemaSetName);
        assertEquals(expectedDataspaceName, schemaSetEntity.getDataspace().getName());
        assertEquals(expectedSchemaSetName, schemaSetEntity.getName());

        // assert the attached yang resource is persisted
        final Set<YangResourceEntity> yangResourceEntities = schemaSetEntity.getYangResources();
        assertNotNull(yangResourceEntities);
        assertEquals(1, yangResourceEntities.size());

        // assert the attached yang resource content
        final YangResourceEntity yangResourceEntity = yangResourceEntities.iterator().next();
        assertNotNull(yangResourceEntity.getId());
        if (expectedYangResourceId != NEW_RESOURCE_ABSTRACT_ID) {
            // existing resource with known id
            assertEquals(expectedYangResourceId, yangResourceEntity.getId());
        }
        assertEquals(expectedYangResourceName, yangResourceEntity.getName());
        assertEquals(expectedYangResourceContent, yangResourceEntity.getContent());
        assertEquals(expectedYangResourceChecksum, yangResourceEntity.getChecksum());
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA_ANCHOR)})
    public void testGetAnchorByDataspaceAndAnchorName() {
        final Anchor anchor = cpsModulePersistenceService.getAnchor(DATASPACE_NAME, ANCHOR_NAME1);

        assertNotNull(anchor);
        assertEquals(ANCHOR_NAME1, anchor.getName());
        assertEquals(DATASPACE_NAME, anchor.getDataspaceName());
    }

    @Test(expected = DataspaceNotFoundException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA_ANCHOR)})
    public void testGetAnchorFromNonExistingDataspace() {
        cpsModulePersistenceService.getAnchor(DATASPACE_NAME_INVALID, ANCHOR_NAME1);
    }

    @Test(expected = AnchorNotFoundException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA_ANCHOR)})
    public void testGetAnchorByNonExistingAnchorName() {
        cpsModulePersistenceService.getAnchor(DATASPACE_NAME, NON_EXISTING_ANCHOR_NAME);
    }

    private static Map<String, String> toMap(final String key, final String value) {
        return ImmutableMap.<String, String>builder().put(key, value).build();
    }

    private SchemaSetEntity getSchemaSetFromDatabase(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.findByName(dataspaceName).orElseThrow();
        return schemaSetRepository.findByDataspaceAndName(dataspaceEntity, schemaSetName).orElseThrow();
    }
}
