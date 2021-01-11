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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onap.cps.spi.CascadeOption.DELETE_DEPENDENT;
import static org.onap.cps.spi.CascadeOption.KEEP_DEPENDENT;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CascadeOption;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.exceptions.DataspaceNotFoundException;
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException;
import org.onap.cps.spi.exceptions.SchemaSetInUseException;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CpsModulePersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/schemaset.sql";

    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String DATASPACE_NAME_INVALID = "DATASPACE-X";
    private static final String SCHEMA_SET_NAME = "SCHEMA-SET-001";
    private static final String SCHEMA_SET_NAME_NEW = "SCHEMA-SET-NEW";
    private static final String SCHEMA_SET_NAME_NO_ANCHORS = "SCHEMA-SET-100";
    private static final String SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA = "SCHEMA-SET-101";

    private static final String EXISTING_RESOURCE_NAME = "module1@2020-02-02.yang";
    private static final String EXISTING_RESOURCE_CONTENT = "CONTENT-001";
    private static final String EXISTING_RESOURCE_CHECKSUM = "877e65a9f36d54e7702c3f073f6bc42b";
    private static final Long EXISTING_RESOURCE_ID = 3001L;

    private static final String NEW_RESOURCE_NAME = "new-module@2020-02-02.yang";
    private static final String NEW_RESOURCE_CONTENT = "CONTENT-NEW";
    private static final String NEW_RESOURCE_CHECKSUM = "c94d40a1350eb1c0b1c1949eac84fc59";
    private static final Long NEW_RESOURCE_ABSTRACT_ID = 0L;

    private static final Long SHARED_RESOURCE_ID1 = 3003L;
    private static final Long SHARED_RESOURCE_ID2 = 3004L;
    private static final Long ORPHAN_RESOURCE_ID = 3100L;
    private static final Integer REMOVED_ANCHOR_ID1 = 6001;
    private static final Integer REMOVED_ANCHOR_ID2 = 6002;
    private static final Long REMOVED_FRAGMENT_ID = 7001L;

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

    @Autowired
    private AnchorRepository anchorRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Test(expected = DataspaceNotFoundException.class)
    @Sql(CLEAR_DATA)
    public void testStoreSchemaSetToInvalidDataspace() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME_INVALID, SCHEMA_SET_NAME_NEW,
            toMap(NEW_RESOURCE_NAME, NEW_RESOURCE_CONTENT));
    }

    @Test(expected = SchemaSetAlreadyDefinedException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDuplicateSchemaSet() {
        cpsModulePersistenceService.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME,
            toMap(NEW_RESOURCE_NAME, NEW_RESOURCE_CONTENT));
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
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
    @Sql({CLEAR_DATA, SET_DATA})
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
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStrictDeleteSchemaSetNoAnchors() {
        cpsModulePersistenceService.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NO_ANCHORS, KEEP_DEPENDENT);

        // validate schema set removed
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME);
        assertFalse(schemaSetRepository
            .findByDataspaceAndName(dataspaceEntity, SCHEMA_SET_NAME_NO_ANCHORS).isPresent());

        // validate shared resource remain, but orphan one is removed
        assertTrue(yangResourceRepository.findById(SHARED_RESOURCE_ID1).isPresent());
        assertFalse(yangResourceRepository.findById(ORPHAN_RESOURCE_ID).isPresent());
    }


    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testFullDeleteSchemaSetWithAnchorsAndData() {
        cpsModulePersistenceService
            .deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA, DELETE_DEPENDENT);

        // validate schema set removed
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME);
        assertFalse(schemaSetRepository
            .findByDataspaceAndName(dataspaceEntity, SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA).isPresent());

        // validate shared resources remain
        assertTrue(yangResourceRepository.findById(SHARED_RESOURCE_ID1).isPresent());
        assertTrue(yangResourceRepository.findById(SHARED_RESOURCE_ID2).isPresent());

        // validate associated anchors and data are removed
        assertFalse(anchorRepository.findById(REMOVED_ANCHOR_ID1).isPresent());
        assertFalse(anchorRepository.findById(REMOVED_ANCHOR_ID2).isPresent());
        assertFalse(fragmentRepository.findById(REMOVED_FRAGMENT_ID).isPresent());
    }

    @Test(expected = DataspaceNotFoundException.class)
    @Sql(CLEAR_DATA)
    public void testDeleteSchemaSetWithinInvalidDataspace() {
        cpsModulePersistenceService.deleteSchemaSet(DATASPACE_NAME_INVALID, SCHEMA_SET_NAME, DELETE_DEPENDENT);
    }

    @Test(expected = SchemaSetNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testDeleteNonExistingSchemaSet() {
        cpsModulePersistenceService.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, DELETE_DEPENDENT);
    }

    @Test(expected = SchemaSetInUseException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStrictDeleteSchemaSetInUse() {
        cpsModulePersistenceService
            .deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA, KEEP_DEPENDENT);
    }

    private static Map<String, String> toMap(final String key, final String value) {
        return ImmutableMap.<String, String>builder().put(key, value).build();
    }

    private SchemaSetEntity getSchemaSetFromDatabase(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.findByName(dataspaceName).orElseThrow();
        return schemaSetRepository.findByDataspaceAndName(dataspaceEntity, schemaSetName).orElseThrow();
    }
}
