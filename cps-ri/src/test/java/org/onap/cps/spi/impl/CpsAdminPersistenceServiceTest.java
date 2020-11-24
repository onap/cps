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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.exceptions.AnchorAlreadyDefinedException;
import org.onap.cps.spi.exceptions.DataspaceNotFoundException;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CpsAdminPersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/anchor.sql";

    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String DATASPACE_NAME_EMPTY = "DATASPACE-002";
    private static final String DATASPACE_NAME_INVALID = "DATASPACE-X";
    private static final String SCHEMA_SET_NAME_INVALID = "SCHEMA-SET-X";
    private static final String SCHEMA_SET_NAME1 = "SCHEMA-SET-001";
    private static final String SCHEMA_SET_NAME2 = "SCHEMA-SET-002";
    private static final String ANCHOR_NAME1 = "ANCHOR-001";
    private static final String ANCHOR_NAME2 = "ANCHOR-002";
    private static final String ANCHOR_NAME_NEW = "ANCHOR-NEW";

    @ClassRule
    public static DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsAdminPersistenceService cpsAdminPersistenceService;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Test(expected = DataspaceNotFoundException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testCreateAnchorAtInvalidDataspace() {
        cpsAdminPersistenceService.createAnchor(DATASPACE_NAME_INVALID, SCHEMA_SET_NAME2, ANCHOR_NAME_NEW);
    }

    @Test(expected = SchemaSetNotFoundException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testCreateAnchorToInvalidSchemaSet() {
        cpsAdminPersistenceService.createAnchor(DATASPACE_NAME, SCHEMA_SET_NAME_INVALID, ANCHOR_NAME_NEW);
    }

    @Test(expected = AnchorAlreadyDefinedException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testCreateAnchorWithNameAlreadyDefined() {
        cpsAdminPersistenceService.createAnchor(DATASPACE_NAME, SCHEMA_SET_NAME2, ANCHOR_NAME1);
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testCreateAnchor() {
        cpsAdminPersistenceService.createAnchor(DATASPACE_NAME, SCHEMA_SET_NAME2, ANCHOR_NAME_NEW);

        // validate anchor persisted
        final Dataspace dataspace = dataspaceRepository.findByName(DATASPACE_NAME).orElseThrow();
        final Fragment anchor =
            fragmentRepository.findByDataspaceAndAnchorName(dataspace, ANCHOR_NAME_NEW).orElseThrow();

        assertNotNull(anchor.getId());
        assertEquals(ANCHOR_NAME_NEW, anchor.getAnchorName());
        assertEquals(DATASPACE_NAME, anchor.getDataspace().getName());
        assertEquals(SCHEMA_SET_NAME2, anchor.getSchemaSet().getName());
    }

    @Test(expected = DataspaceNotFoundException.class)
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetAnchorsByInvalidDataspace() {
        cpsAdminPersistenceService.getAnchors(DATASPACE_NAME_INVALID);
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetAnchorsFromEmptyDataspace() {
        final Collection<Anchor> anchors = cpsAdminPersistenceService.getAnchors(DATASPACE_NAME_EMPTY);

        assertNotNull(anchors);
        assertTrue(anchors.isEmpty());
    }

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testGetAnchorsByDataspace() {
        final Collection<Anchor> anchors = cpsAdminPersistenceService.getAnchors(DATASPACE_NAME);

        assertNotNull(anchors);
        assertEquals(2, anchors.size());
        assertTrue(anchors.contains(
            Anchor.builder().name(ANCHOR_NAME1).schemaSetName(SCHEMA_SET_NAME1).dataspaceName(DATASPACE_NAME).build()
        ));
        assertTrue(anchors.contains(
            Anchor.builder().name(ANCHOR_NAME2).schemaSetName(SCHEMA_SET_NAME2).dataspaceName(DATASPACE_NAME).build()
        ));
    }

}
