/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications (C) 2021 Nordix Foundation
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

package org.onap.cps.rest.controller;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.rest.api.CpsDataApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
public class DataRestController implements CpsDataApi {

    @Autowired
    private CpsDataService cpsDataService;

    @Override
    public ResponseEntity<String> createNode(@Valid final String jsonData, @NotNull final String dataspaceName,
        @NotNull @Valid final String anchorName) {
        cpsDataService.saveData(dataspaceName, anchorName, jsonData);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Object> getNodesByDataspace(final String dataspaceName) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getNodeByDataspaceAndAnchor(final String dataspaceName, final String anchorName,
        final String xpath, final Boolean includeDescendants) {
        if ("/".equals(xpath)) {
            return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        }
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final DataNode dataNode =
            cpsDataService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption);
        return new ResponseEntity<>(DataMapUtils.toDataMap(dataNode), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> updateNodeLeaves(final String jsonData, final String dataspaceName,
        final String anchorName, final String parentNodeXpath) {
        cpsDataService.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath, jsonData);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> replaceNode(final String jsonData, final String dataspaceName,
        final String anchorName, final String parentNodeXpath) {
        cpsDataService.replaceNodeTree(dataspaceName, anchorName, parentNodeXpath, jsonData);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
