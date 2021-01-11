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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Override
    public void storeDataNode(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final Dataspace dataspace = dataspaceRepository.findByName(dataspaceName).orElseThrow();
        final Fragment anchor =
            fragmentRepository.getByDataspaceAndAnchorName(dataspace, anchorName);
        final List<Fragment> fragmentList = new LinkedList<>();
        fragmentList.add(createFragment(dataspace, anchor, dataNode, fragmentList));
        fragmentRepository.saveAll(fragmentList);
    }

    /**
     * Create a fragment from the given DataNode.
     *
     * @param dataspace    dataspace
     * @param anchor       anchor
     * @param dataNode     dataNode
     * @param fragmentList a list of Fragments
     * @return a Fragment
     */
    private static Fragment createFragment(final Dataspace dataspace, final Fragment anchor,
        final DataNode dataNode,
        final List<Fragment> fragmentList) {
        final Fragment fragment = Fragment.builder()
            .dataspace(dataspace)
            .anchorName(anchor.getAnchorName())
            .xpath(dataNode.getXpath())
            .attributes(getLeavesAsJson(dataNode.getLeaves()))
            .build();

        for (final DataNode child : dataNode.getChildDataNodes()) {
            final Fragment childFragment = createFragment(dataspace, anchor, child, fragmentList);
            childFragment.setParentFragment(fragment);
            fragmentList.add(childFragment);
        }
        return fragment;
    }

    /**
     * Convert leaves to json string.
     *
     * @param leaves the DataNode leaves
     * @return a json string
     */
    public static String getLeavesAsJson(final Map<String, Object> leaves) {
        final Gson gson = new Gson();
        final Type gsonType = new TypeToken<HashMap>() {
        }.getType();
        final String gsonString = gson.toJson(leaves, gsonType);
        return gsonString;
    }
}
