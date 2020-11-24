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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.api.model.ModuleDescriptor;
import org.onap.cps.api.model.ModuleSetDescriptor;
import org.onap.cps.exceptions.CpsNotFoundException;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.spi.repository.ModuleRepository;
import org.onap.cps.spi.repository.ModuleSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class ModelPersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/moduleset.sql";

    private static final String DATASPACE_NAME1 = "dataspace-001";
    private static final String DATASPACE_NAME2 = "dataspace-002";
    private static final String DATASPACE_NAME_INVALID = "dataspace-X";
    private static final String MODULESET_NAME1 = "moduleset-001";
    private static final String MODULESET_NAME2 = "moduleset-002";
    private static final String MODULESET_NAME3 = "moduleset-003";
    private static final String MODULESET_NAME_INVALID = "moduleset-X";

    private static final String MODULE_NAMESPACE = "http://cps.onap.org/test/";
    private static final String MODULE_REVISION = "2020-10-10";
    private static final String MODULE_CONTENT = "CONTENT-NEW";

    @ClassRule
    public static DatabaseTestContainer testContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private ModelPersistenceService modelPersistenceService;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleSetRepository moduleSetRepository;


    @Test(expected = CpsNotFoundException.class)
    @Sql(CLEAR_DATA)
    public void testGetAllModuleSetsByInvalidDataspace() {
        List<ModuleSetDescriptor> moduleSets = modelPersistenceService.getAllModuleSets(DATASPACE_NAME_INVALID);
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetAllModuleSetsByDataspace() {
        List<ModuleSetDescriptor> moduleSets = modelPersistenceService.getAllModuleSets(DATASPACE_NAME1);

        assertNotNull(moduleSets);
        assertEquals(2, moduleSets.size());
        assertTrue(moduleSets.contains(
            ModuleSetDescriptor.builder().dataspace(DATASPACE_NAME1).name(MODULESET_NAME1).build()
        ));
        assertTrue(moduleSets.contains(
            ModuleSetDescriptor.builder().dataspace(DATASPACE_NAME1).name(MODULESET_NAME2).build()
        ));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetEmptyModuleSetList() {
        List<ModuleSetDescriptor> moduleSets = modelPersistenceService.getAllModuleSets(DATASPACE_NAME2);
        assertNotNull(moduleSets);
        assertTrue(moduleSets.isEmpty());
    }


    @Test(expected = CpsNotFoundException.class)
    @Sql(CLEAR_DATA)
    public void testGetModuleSetByInvalidDataspace() {
        modelPersistenceService.getModuleSet(DATASPACE_NAME_INVALID, MODULESET_NAME1);
    }

    @Test(expected = CpsNotFoundException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetModuleSetByInvalidName() {
        modelPersistenceService.getModuleSet(DATASPACE_NAME1, MODULESET_NAME_INVALID);
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetModuleSetByName() {
        ModuleSetDescriptor result = modelPersistenceService.getModuleSet(DATASPACE_NAME1, MODULESET_NAME1);

        assertNotNull(result);
        assertEquals(DATASPACE_NAME1, result.getDataspace());
        assertEquals(MODULESET_NAME1, result.getName());

        assertNotNull(result.getModules());
        assertEquals(2, result.getModules().size());

        assertTrue(result.getModules().contains(
            ModuleDescriptor.builder().dataspace(DATASPACE_NAME1).moduleset(MODULESET_NAME1)
                .namespace(MODULE_NAMESPACE).revision("2020-01-01").content("CONTENT-001").build()
        ));
        assertTrue(result.getModules().contains(
            ModuleDescriptor.builder().dataspace(DATASPACE_NAME1).moduleset(MODULESET_NAME1)
                .namespace(MODULE_NAMESPACE).revision("2020-02-02").content("CONTENT-002").build()
        ));
    }


    @Test(expected = CpsValidationException.class)
    @Sql(CLEAR_DATA)
    public void testStoreModuleSetToInvalidDataspace() {
        modelPersistenceService.storeModuleSet(buildModuleSetToStore(DATASPACE_NAME_INVALID, MODULESET_NAME3));
    }

    @Test(expected = CpsValidationException.class)
    @Sql(CLEAR_DATA)
    public void testStoreEmptyModuleSet() {
        modelPersistenceService.storeModuleSet(
            ModuleSetDescriptor.builder().dataspace(DATASPACE_NAME1).name(MODULESET_NAME3).build()
        );
    }

    @Test(expected = CpsValidationException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreDuplicateModuleSet() {
        modelPersistenceService.storeModuleSet(buildModuleSetToStore(DATASPACE_NAME1, MODULESET_NAME1));
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreModuleSet() {
        modelPersistenceService.storeModuleSet(buildModuleSetToStore(DATASPACE_NAME1, MODULESET_NAME3));

        // validate the data is persisted
        ModuleSetDescriptor result = modelPersistenceService.getModuleSet(DATASPACE_NAME1, MODULESET_NAME3);

        assertNotNull(result);
        assertEquals(DATASPACE_NAME1, result.getDataspace());
        assertEquals(MODULESET_NAME3, result.getName());
        assertNotNull(result.getModules());
        assertEquals(1, result.getModules().size());

        assertTrue(result.getModules().contains(
            ModuleDescriptor.builder()
                .dataspace(DATASPACE_NAME1).moduleset(MODULESET_NAME3)
                .namespace(MODULE_NAMESPACE).revision(MODULE_REVISION).content(MODULE_CONTENT)
                .build()
        ));
    }

    @Test(expected = CpsValidationException.class)
    @Sql(CLEAR_DATA)
    public void testDeleteModuleSetByInvalidDataspace() {
        modelPersistenceService.deleteModuleSet(DATASPACE_NAME_INVALID, MODULESET_NAME1);
    }

    @Test(expected = CpsValidationException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testDeleteModuleSetByInvalidName() {
        modelPersistenceService.deleteModuleSet(DATASPACE_NAME1, MODULESET_NAME_INVALID);
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testDeleteModuleSet() {
        modelPersistenceService.deleteModuleSet(DATASPACE_NAME1, MODULESET_NAME2);
        // validate the data is no longer in database
        assertFalse(moduleSetRepository.existsById(2002));
        assertFalse(moduleRepository.existsById(3003));
        assertFalse(moduleRepository.existsById(3004));
    }

    private static ModuleSetDescriptor buildModuleSetToStore(String dataspaceName, String moduleSetName) {
        return ModuleSetDescriptor.builder()
            .dataspace(dataspaceName)
            .name(moduleSetName)
            .modules(
                new HashSet<>(Arrays.asList(
                    ModuleDescriptor.builder()
                        .revision(MODULE_REVISION)
                        .namespace(MODULE_NAMESPACE)
                        .content(MODULE_CONTENT).build()
                ))
            ).build();
    }
}
