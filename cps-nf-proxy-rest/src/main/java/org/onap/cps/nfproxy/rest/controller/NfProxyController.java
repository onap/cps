/*
 *  ============LICENSE_START=======================================================
 *  ONAP CPS
 *  ================================================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Update Copyright (C) 2021 highstreet technologies GmbH Intellectual Property.
 *  All rights reserved.
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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.nfproxy.rest.controller;

import com.google.gson.Gson;
import javax.validation.Valid;
import org.onap.cps.api.NfProxyDataService;
import org.onap.cps.nfproxy.rest.api.NfProxyApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("${rest.api.xnf-base-path}")
public class NfProxyController implements NfProxyApi {

    @Autowired
    private NfProxyDataService nfProxyDataService;

    static final String ROOT = "/";

    @Override
    public ResponseEntity<Object> getNodeByCmHandleIdAndXpath(final String cmHandleId, @Valid final String cpsPath,
                                                              @Valid final Boolean includeDescendants) {
        if (ROOT.equals(cpsPath)) {
            return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        }
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final DataNode dataNode = nfProxyDataService.getDataNode(cmHandleId, cpsPath, fetchDescendantsOption);
        return new ResponseEntity<>(new Gson().toJson(dataNode), HttpStatus.OK);
    }
}
