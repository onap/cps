/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import static com.google.common.collect.MoreCollectors.onlyElement;
import static junit.framework.TestCase.assertEquals;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.NoSuchElementException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.DataspaceNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CpsDataPersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/fragment.sql";

    private static final String NON_EXISTING_DATASPACE_NAME = "NON EXISTING DATASPACE";
    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String ANCHOR_NAME1 = "ANCHOR-001";
    private static final String NON_EXISTING_ANCHOR_NAME = "NON EXISTING ANCHOR";
    private static final String PARENT_XPATH = "/parent";
    private static final String CHILD_XPATH = "/parent/child";
    private static final String GRAND_CHILD_XPATH = "/parent/child/grandchild";
    private static final String PARENT_XPATH_NEW = "/parent-new";
    private static final String CHILD_XPATH_NEW = "/parent/child-new";
    private static final String GRAND_CHILD_XPATH_NEW = "/parent/child/grandchild-new";

    @ClassRule
    public static DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testGetFragmentsWithChildAndGrandChild() {
        final FragmentEntity parentFragment = fragmentRepository.findAll().stream()
            .filter(fragment -> fragment.getXpath().contains(PARENT_XPATH))
            .findAny().orElseThrow();
        final FragmentEntity childFragment = parentFragment.getChildFragments().stream()
            .filter(fragment -> fragment.getXpath().contains(CHILD_XPATH)).findAny().orElseThrow();
        final FragmentEntity grandChildFragment = childFragment.getChildFragments().stream()
            .filter(fragment -> fragment.getXpath().contains(GRAND_CHILD_XPATH)).findAny().orElseThrow();

        assertEquals(GRAND_CHILD_XPATH, grandChildFragment.getXpath());
        assertEquals(DATASPACE_NAME, grandChildFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, grandChildFragment.getAnchor().getName());

        assertEquals(CHILD_XPATH, childFragment.getXpath());
        assertEquals(DATASPACE_NAME, childFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, childFragment.getAnchor().getName());

        assertEquals(PARENT_XPATH, parentFragment.getXpath());
        assertEquals(DATASPACE_NAME, parentFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, parentFragment.getAnchor().getName());

    }

    @Test(expected = DataspaceNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeAtNonExistingDataspace() {
        cpsDataPersistenceService
            .storeDataNode(NON_EXISTING_DATASPACE_NAME, ANCHOR_NAME1,
                createDataNodeWithChildAndGrandChild(PARENT_XPATH_NEW, CHILD_XPATH_NEW, GRAND_CHILD_XPATH_NEW));
    }

    @Test(expected = NoSuchElementException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeAtNonExistingAnchor() {
        cpsDataPersistenceService
            .storeDataNode(DATASPACE_NAME, NON_EXISTING_ANCHOR_NAME,
                createDataNodeWithChildAndGrandChild(PARENT_XPATH_NEW, CHILD_XPATH_NEW, GRAND_CHILD_XPATH_NEW));
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeWithIntegrityException() {
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
            createDataNodeWithChildAndGrandChild(PARENT_XPATH, CHILD_XPATH, GRAND_CHILD_XPATH));
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeWithChildrenAndGrandChildren() {
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
            createDataNodeWithChildAndGrandChild(PARENT_XPATH_NEW, CHILD_XPATH_NEW, GRAND_CHILD_XPATH_NEW));

        final FragmentEntity parentFragment = fragmentRepository.findAll().stream()
            .filter(fragment -> fragment.getXpath().contains(PARENT_XPATH_NEW))
            .collect(onlyElement());
        final FragmentEntity childFragment = parentFragment.getChildFragments().stream()
            .filter(fragment -> fragment.getXpath().contains(CHILD_XPATH_NEW)).findAny().orElseThrow();
        final FragmentEntity grandChildFragment = childFragment.getChildFragments().stream()
            .filter(fragment -> fragment.getXpath().contains(GRAND_CHILD_XPATH_NEW)).findAny().orElseThrow();

        assertEquals(GRAND_CHILD_XPATH_NEW, grandChildFragment.getXpath());
        assertEquals(DATASPACE_NAME, grandChildFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, grandChildFragment.getAnchor().getName());

        assertEquals(CHILD_XPATH_NEW, childFragment.getXpath());
        assertEquals(DATASPACE_NAME, childFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, childFragment.getAnchor().getName());

        assertEquals(PARENT_XPATH_NEW, parentFragment.getXpath());
        assertEquals(DATASPACE_NAME, parentFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, parentFragment.getAnchor().getName());
    }

    private DataNode createDataNodeWithChildAndGrandChild(final String parentXpath, final String childXpath,
        final String grandChildXpath) {
        final DataNode parentDataNode = DataNode.builder()
            .xpath(parentXpath)
            .build();

        final DataNode childDataNode = DataNode.builder()
            .xpath(childXpath)
            .childDataNodes(Collections.emptySet())
            .build();

        final DataNode grandChildDataNode = DataNode.builder()
            .xpath(grandChildXpath)
            .childDataNodes(Collections.emptySet())
            .build();

        childDataNode.setChildDataNodes(ImmutableSet.of(grandChildDataNode));
        parentDataNode.setChildDataNodes(ImmutableSet.of(childDataNode));
        return parentDataNode;
    }
}
