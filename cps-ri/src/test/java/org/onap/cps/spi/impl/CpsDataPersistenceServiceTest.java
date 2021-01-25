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
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.AnchorNotFoundException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataspaceNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
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

    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String ANCHOR_NAME1 = "ANCHOR-001";

    private static final long PARENT_ID_4001 = 4001;
    private static final long PARENT_ID_4002 = 4002;
    private static final long PARENT_ID_4003 = 4003;
    private static final String PARENT_XPATH1 = "/parent-1";
    private static final String PARENT_XPATH2 = "/parent-2";
    private static final String PARENT_XPATH3 = "/parent-3";

    private static final long CHILD_ID_4004 = 4004;
    private static final String CHILD_XPATH1 = "/parent-1/child-1";
    private static final String CHILD_XPATH2 = "/parent-2/child-2";

    private static final long GRAND_CHILD_ID_4006 = 4006;
    private static final String GRAND_CHILD_XPATH1 = "/parent-1/child-1/grandchild-1";

    @ClassRule
    public static DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testGetFragmentsWithChildAndGrandChild() {
        final FragmentEntity parentFragment = fragmentRepository.findById(PARENT_ID_4001).orElseThrow();
        final FragmentEntity childFragment = fragmentRepository.findById(CHILD_ID_4004).orElseThrow();
        final FragmentEntity grandChildFragment = fragmentRepository.findById(GRAND_CHILD_ID_4006).orElseThrow();
        assertFragment(parentFragment, childFragment, grandChildFragment, PARENT_XPATH1, CHILD_XPATH1,
            GRAND_CHILD_XPATH1);
    }

    @Test(expected = DataspaceNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeAtNonExistingDataspace() {
        cpsDataPersistenceService
            .storeDataNode("Non Existing Dataspace Name", ANCHOR_NAME1,
                    new DataNodeBuilder().build());
    }

    @Test(expected = AnchorNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeAtNonExistingAnchor() {
        cpsDataPersistenceService
            .storeDataNode(DATASPACE_NAME, "Non Existing Anchor Name",
                    new DataNodeBuilder().build());
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeWithIntegrityException() {
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
            createDataNodeTree(PARENT_XPATH1));
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testStoreDataNodeWithChildAndGrandChild() {
        final String parentXpath = "/parent-new";
        final String childXpath = "/parent-new/child-new";
        final String grandChildXpath = "/parent-new/child-new/grandchild-new";

        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
            createDataNodeTree(parentXpath, childXpath, grandChildXpath));
        final FragmentEntity parentFragment = getFragmentByXpath(parentXpath);
        final FragmentEntity childFragment = getFragmentByXpath(childXpath);
        final FragmentEntity grandChildFragment = getFragmentByXpath(grandChildXpath);
        assertFragment(parentFragment, childFragment, grandChildFragment, parentXpath, childXpath,
            grandChildXpath);
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testAddChildToFragmentThatHasOneChild() {
        final String childXpath = "some-xpath";
        final DataNode childDataNode = createDataNodeTree(childXpath);
        cpsDataPersistenceService
            .addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, PARENT_XPATH2, childDataNode);
        final FragmentEntity parentFragment = fragmentRepository.findById(PARENT_ID_4002).orElseThrow();
        Assertions.assertThat(parentFragment.getChildFragments())
            .hasSize(2)
            .extracting(FragmentEntity::getXpath)
            .containsExactlyInAnyOrder(childXpath, CHILD_XPATH2);
    }

    @Test
    @Sql({CLEAR_DATA, SET_DATA})
    public void testAddChildToFragmentThatHasNoChild() {
        final String childXpath = "some-xpath";
        final DataNode childDataNode = createDataNodeTree(childXpath);
        cpsDataPersistenceService
            .addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, PARENT_XPATH3, childDataNode);
        final FragmentEntity parentFragment = fragmentRepository.findById(PARENT_ID_4003).orElseThrow();
        Assertions.assertThat(parentFragment.getChildFragments())
            .hasSize(1)
            .extracting(FragmentEntity::getXpath)
            .containsExactlyInAnyOrder(childXpath);
    }

    @Test(expected = DataIntegrityViolationException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testAddAChildWithTheSameXpathAsExistingChild() {
        cpsDataPersistenceService
            .addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, PARENT_XPATH1, createDataNodeTree(CHILD_XPATH1));
    }

    @Test(expected = DataNodeNotFoundException.class)
    @Sql({CLEAR_DATA, SET_DATA})
    public void testAddAChildWithToAParentThatDoesNotExist() {
        cpsDataPersistenceService
            .addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, "non-existing-xpath", createDataNodeTree("some-xpath"));
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

    private FragmentEntity getFragmentByXpath(final String xpath) {
        return fragmentRepository.findAll().stream()
            .filter(fragment -> fragment.getXpath().contains(xpath)).findAny().orElseThrow();
    }

    private static DataNode createDataNodeTree(final String... xpaths) {
        ImmutableSet<DataNode> childNodes = null;
        if (xpaths.length > 1) {
            final String[] xPathsDescendant = Arrays.copyOfRange(xpaths, 1, xpaths.length);
            final DataNode childDataNode = createDataNodeTree(xPathsDescendant);
            childNodes = ImmutableSet.of(childDataNode);
        }
        return new DataNodeBuilder().withXpath(xpaths[0]).withChildDataNodes(childNodes).build();
    }
}
