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

import static junit.framework.TestCase.assertEquals;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.AnchorNotFoundException;
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
    private static final String CHILD_XPATH_NEW1 = "/parent/child-1-new";
    private static final String CHILD_XPATH_NEW2 = "/parent/child-2-new";
    private static final String GRAND_CHILD_XPATH_NEW = "/parent/child/grandchild-new";
    private static final long PARENT_ID = 3001;
    private static final long CHILD_ID = 3002;
    private static final long GRAND_CHILD_ID = 3003;
    private static final long PARENT_ID_NEW = 2;
    private static final long CHILD_ID_NEW = 3;
    private static final long GRAND_CHILD_ID_NEW = 4;

    @ClassRule
    public static DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testGetFragmentsWithChildAndGrandChild() {
        final FragmentEntity parentFragment = fragmentRepository.findById(PARENT_ID).orElseThrow();
        final FragmentEntity childFragment = fragmentRepository.findById(CHILD_ID).orElseThrow();
        final FragmentEntity grandChildFragment = fragmentRepository.findById(GRAND_CHILD_ID).orElseThrow();

        assertFragment(parentFragment, childFragment, grandChildFragment, PARENT_XPATH, CHILD_XPATH, GRAND_CHILD_XPATH);
    }

    @Test(expected = DataspaceNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeAtNonExistingDataspace() {
        cpsDataPersistenceService
            .storeDataNode(NON_EXISTING_DATASPACE_NAME, ANCHOR_NAME1,
                createDataNodeWithChildAndGrandChild(PARENT_XPATH_NEW, CHILD_XPATH_NEW1, GRAND_CHILD_XPATH_NEW));
    }

    @Test(expected = AnchorNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeAtNonExistingAnchor() {
        cpsDataPersistenceService
            .storeDataNode(DATASPACE_NAME, NON_EXISTING_ANCHOR_NAME,
                createDataNodeWithChildAndGrandChild(PARENT_XPATH_NEW, CHILD_XPATH_NEW1, GRAND_CHILD_XPATH_NEW));
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeWithIntegrityException() {
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
            createDataNodeWithChildAndGrandChild(PARENT_XPATH, CHILD_XPATH, GRAND_CHILD_XPATH));
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeWithChildAndGrandChild() {
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
            createDataNodeWithChildAndGrandChild(PARENT_XPATH_NEW, CHILD_XPATH_NEW1, GRAND_CHILD_XPATH_NEW));

        final FragmentEntity parentFragment = fragmentRepository.findById(PARENT_ID_NEW).orElseThrow();
        final FragmentEntity childFragment = fragmentRepository.findById(CHILD_ID_NEW).orElseThrow();
        final FragmentEntity grandChildFragment = fragmentRepository.findById(GRAND_CHILD_ID_NEW).orElseThrow();

        assertFragment(parentFragment, childFragment, grandChildFragment, PARENT_XPATH_NEW, CHILD_XPATH_NEW1,
            GRAND_CHILD_XPATH_NEW);
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testAddChildToFragmentThatHasOneChild() {
        final DataNode parentDataNode = createDataNode(PARENT_XPATH_NEW);
        final DataNode childDataNode1 = createDataNode(CHILD_XPATH_NEW1);
        final DataNode childDataNode2 = createDataNode(CHILD_XPATH_NEW2);

        parentDataNode.setChildDataNodes(ImmutableSet.of(childDataNode1));
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentDataNode);

        cpsDataPersistenceService
            .addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentDataNode.getXpath(), childDataNode2);

        final FragmentEntity parentFragment = fragmentRepository.findById((long) 5).orElseThrow();

        Assertions.assertThat(parentFragment.getChildFragments())
            .hasSize(2)
            .extracting(FragmentEntity::getXpath)
            .containsExactlyInAnyOrder(CHILD_XPATH_NEW1, CHILD_XPATH_NEW2);
    }

    private DataNode createDataNode(final String xpath) {
        final DataNode dataNode = DataNode.builder()
            .xpath(xpath)
            .childDataNodes(Collections.emptySet())
            .build();

        return dataNode;
    }

    private void assertFragment(final FragmentEntity parentFragment, final FragmentEntity childFragment,
        final FragmentEntity grandChildFragment, final String parentXpath, final String childXpath,
        final String grandChildXpath) {
        assertEquals(parentXpath, parentFragment.getXpath());
        assertEquals(DATASPACE_NAME, parentFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, parentFragment.getAnchor().getName());

        assertEquals(childXpath, childFragment.getXpath());
        assertEquals(DATASPACE_NAME, childFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, childFragment.getAnchor().getName());

        assertEquals(grandChildXpath, grandChildFragment.getXpath());
        assertEquals(DATASPACE_NAME, grandChildFragment.getDataspace().getName());
        assertEquals(ANCHOR_NAME1, grandChildFragment.getAnchor().getName());
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

        parentDataNode.setChildDataNodes(ImmutableSet.of(childDataNode));
        childDataNode.setChildDataNodes(ImmutableSet.of(grandChildDataNode));
        return parentDataNode;
    }
}
