/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.api.impl;

import java.io.IOException;
import java.util.Map;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CpsDataServiceImpl implements CpsDataService {

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private CpsModuleService cpsModuleService;

    @Override
    public void parseAndValidateYangData(final Map<String, String> jsonDataMap, final String dataspaceName,
        final String anchorName) {
        final Anchor anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final SchemaContext schemaContext = cpsModuleService.getSchemaContext(dataspaceName, anchor.getSchemaSetName());
        final Map.Entry<String, String> jsonData = jsonDataMap.entrySet().iterator().next();
        try {
            final NormalizedNode normalizedNode = YangUtils.parseJsonData(jsonData.getValue(), schemaContext);
            final DataNode dataNode = new DataNodeBuilder().withNormalizedNodeTree(normalizedNode).build();
            cpsDataPersistenceService.storeDataNode(dataspaceName, anchor.getName(), dataNode);
        } catch (final IOException e) {
            throw new CpsException("Failed to parse yang resource.", String
                .format("Exception occurred on parsing resource %s.", jsonData), e);
        }
    }
}