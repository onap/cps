/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.cps.DatabaseTestContainer;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
public class CpsDataPersistenceServiceTest {

    private static final String CLEAR_DATA = "/data/clear-all.sql";
    private static final String SET_DATA = "/data/anchor.sql";

    private static final String DATASPACE_NAME = "DATASPACE-001";
    private static final String ANCHOR_NAME1 = "ANCHOR-001";


    @ClassRule
    public static DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance();

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Test
    @SqlGroup({@Sql(CLEAR_DATA), @Sql(SET_DATA)})
    public void testStoreNodes() {
        final DataNode parentDataNode = DataNode.builder()
            .xpath("/Parent")
            .build();

        final DataNode childDataNode = DataNode.builder()
            .xpath("/Child")
            .build();

        createChildDataNode(parentDataNode, childDataNode.getXpath());
        cpsDataPersistenceService.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentDataNode);

        final Fragment childFragment = fragmentRepository.findAll().stream()
            .filter(fragment -> fragment.getParentFragment() != null)
            .collect(onlyElement());

        assertEquals("/Parent/Child", childFragment.getXpath());
        assertEquals("/Parent", childFragment.getParentFragment().getXpath());
    }

    private DataNode createChildDataNode(final DataNode parentDataNode, final String childDataNode) {
        final DataNode dataNode = DataNode.builder()
            .dataspace(parentDataNode.getDataspace())
            .anchorName(parentDataNode.getAnchorName())
            .moduleReference(parentDataNode.getModuleReference())
            .xpath(parentDataNode.getXpath() + childDataNode)
            .build();

        parentDataNode.getChildDataNodes().add(dataNode);
        return dataNode;
    }
}
