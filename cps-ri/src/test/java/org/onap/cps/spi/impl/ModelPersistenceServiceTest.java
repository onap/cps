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
import org.onap.cps.spi.model.SchemaSet;
import org.onap.cps.spi.model.YangFile;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangFileEntity;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangFileRepository;
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

    private static final String DATASPACE_NAME1 = "DATASPACE-001";
    private static final String DATASPACE_NAME_INVALID = "DATASPACE-X";
    private static final String SCHEMASET_NAME1 = "SCHEMASET-001";
    private static final String SCHEMASET_NAME_NEW = "SCHEMASET-NEW";
    private static final String OLD_FILE_CONTENT = "CONTENT-001";
    private static final String NEW_FILE_NAME = "new.jang";
    private static final String NEW_FILE_CONTENT = "CONTENT-NEW";
    private static final String NEW_FILE_CHECKSUM = "c94d40a1350eb1c0b1c1949eac84fc59";

    @ClassRule
    public static DatabaseTestContainer testContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private ModelPersistenceService modelPersistenceService;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private YangFileRepository yangFileRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;


    @Test(expected = CpsValidationException.class)
    @Sql(CLEAR_DATA)
    public void testStoreSchemaSetToInvalidDataspace() {
        modelPersistenceService.storeSchemaSet(buildSchemaSetToStore(DATASPACE_NAME_INVALID, SCHEMASET_NAME_NEW));
    }

    @Test(expected = CpsValidationException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreEmptySchemaSet() {
        modelPersistenceService.storeSchemaSet(
            SchemaSet.builder().dataspace(DATASPACE_NAME1).name(SCHEMASET_NAME_NEW).build()
        );
    }

    @Test(expected = CpsValidationException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreDuplicateSchemaSet() {
        modelPersistenceService.storeSchemaSet(buildSchemaSetToStore(DATASPACE_NAME1, SCHEMASET_NAME1));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSet() {
        modelPersistenceService.storeSchemaSet(buildSchemaSetToStore(DATASPACE_NAME1, SCHEMASET_NAME_NEW));

        // validate the data is persisted
        SchemaSetEntity schemaSetEntity = getEntityFromDatabase(DATASPACE_NAME1, SCHEMASET_NAME_NEW);
        assertEquals(DATASPACE_NAME1, schemaSetEntity.getDataspace().getName());
        assertEquals(SCHEMASET_NAME_NEW, schemaSetEntity.getName());

        Set<YangFileEntity> yangFileEntities = schemaSetEntity.getYangFiles();
        assertNotNull(yangFileEntities);
        assertEquals(1, yangFileEntities.size());

        YangFileEntity yangFileEntity = yangFileEntities.iterator().next();
        assertEquals(NEW_FILE_NAME, yangFileEntity.getName());
        assertEquals(NEW_FILE_CONTENT, yangFileEntity.getContent());
        assertEquals(NEW_FILE_CHECKSUM, yangFileEntity.getChecksum());
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreSchemaSetReuseFiles() throws Exception {

        SchemaSet schemaSet = buildSchemaSetToStore(DATASPACE_NAME1, SCHEMASET_NAME_NEW);
        schemaSet.setYangFiles(new HashSet<>(Arrays.asList(
            YangFile.builder().name("new-file.yang").content(OLD_FILE_CONTENT).build()
        )));
        modelPersistenceService.storeSchemaSet(schemaSet);

        // validate the data is persisted
        SchemaSetEntity schemaSetEntity = getEntityFromDatabase(DATASPACE_NAME1, SCHEMASET_NAME_NEW);
        assertEquals(DATASPACE_NAME1, schemaSetEntity.getDataspace().getName());
        assertEquals(SCHEMASET_NAME_NEW, schemaSetEntity.getName());

        Set<YangFileEntity> yangFileEntities = schemaSetEntity.getYangFiles();
        assertNotNull(yangFileEntities);
        assertEquals(1, yangFileEntities.size());

        YangFileEntity yangFileEntity = yangFileEntities.iterator().next();
        assertEquals("file-001.yang", yangFileEntity.getName());
        assertEquals(OLD_FILE_CONTENT, yangFileEntity.getContent());
        assertEquals("877e65a9f36d54e7702c3f073f6bc42b", yangFileEntity.getChecksum());
    }

    private static SchemaSet buildSchemaSetToStore(String dataspaceName, String moduleSetName) {
        YangFile yangFile = YangFile.builder()
            .name(NEW_FILE_NAME).content(NEW_FILE_CONTENT).build();
        return SchemaSet.builder().dataspace(dataspaceName).name(moduleSetName)
            .yangFiles(new HashSet<>(Arrays.asList(yangFile))).build();
    }

    private SchemaSetEntity getEntityFromDatabase(String dataspaceName, String moduleSetName) {
        Dataspace dataspaceEntity = dataspaceRepository.findByName(DATASPACE_NAME1).orElseThrow();
        return schemaSetRepository.findByDataspaceAndName(dataspaceEntity, SCHEMASET_NAME_NEW).orElseThrow();
    }
}
