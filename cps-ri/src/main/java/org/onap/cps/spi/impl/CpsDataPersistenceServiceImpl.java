/*
 * ============LICENSE_START=======================================================
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.LinkedList;
import java.util.List;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpsDataPersistenceServiceImpl implements CpsDataPersistenceService {

    private static Gson gson = new GsonBuilder().create();

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final Fragment anchor =
            fragmentRepository.getByDataspaceAndAnchorName(dataspace, anchorName);
        final Gson gson = new GsonBuilder().create();
        final List<Fragment> fragmentList = new LinkedList<>();
        collectFragmentsRecursively(dataspace, anchor, dataNode, fragmentList, gson);
        fragmentRepository.saveAll(fragmentList);
    }

    /**
     * Convert DataNode object into Fragment and places result into list. Performs same action for all DataNode children
     * recursively.
     *
     * @param dataspace    dataspace
     * @param anchor       anchor
     * @param dataNode     dataNode
     * @param fragmentList a list of Fragments
     * @param gson         Gson instance for attributes conversion
     * @return a Fragment built from current DataNode
     */
    private static Fragment collectFragmentsRecursively(final Dataspace dataspace, final Fragment anchor,
        final DataNode dataNode, final List<Fragment> fragmentList, final Gson gson) {

        final Fragment fragment = Fragment.builder()
            .dataspace(dataspace)
            .anchorName(anchor.getAnchorName())
            .xpath(dataNode.getXpath())
            .attributes(gson.toJson(dataNode.getLeaves()))
            .build();
        fragmentList.add(fragment);

        for (final DataNode childDataNode : dataNode.getChildDataNodes()) {
            final Fragment childFragment =
                collectFragmentsRecursively(dataspace, anchor, childDataNode, fragmentList, gson);
            childFragment.setParentFragment(fragment);
        }
        return fragment;
    }
}
