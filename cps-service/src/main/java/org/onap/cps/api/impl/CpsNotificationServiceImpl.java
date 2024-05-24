/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsNotificationService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.CpsNotificationPersistenceService;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.utils.CpsValidator;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.YangParser;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsNotificationServiceImpl implements CpsNotificationService {

    private final CpsNotificationPersistenceService cpsNotificationPersistenceService;

    private final CpsAnchorService cpsAnchorService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private final YangParser yangParser;

    private static final String ADMIN_DATASPACE = "CPS-Admin";
    private static final String ANCHOR_NAME = "cps-notification-subscriptions";

    @Override
    public void createNotificationSubscription(final String jsonData, final String xpath) {
        final Anchor anchor = cpsAnchorService.getAnchor(ADMIN_DATASPACE, ANCHOR_NAME);
        final Collection<DataNode> dataNodes = buildDataNodes(anchor, jsonData, ContentType.JSON);
        cpsDataPersistenceService.addChildDataNodes(ADMIN_DATASPACE, ANCHOR_NAME, xpath, dataNodes);
    }

    @Override
    public void deleteNotificationSubscription(final String xpath) {
        cpsDataPersistenceService.deleteDataNode(ADMIN_DATASPACE, ANCHOR_NAME, xpath);
    }


    @Override
    public boolean isNotificationEnabled(final String dataspaceName, final String anchorName) {
        return cpsNotificationPersistenceService.isNotificationSubscribed(dataspaceName, anchorName);
    }

    private Collection<DataNode> buildDataNodes(final Anchor anchor,
                                                final String nodeData, final ContentType contentType) {
        final ContainerNode containerNode = yangParser.parseData(contentType, nodeData, anchor, "");
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
                .withContainerNode(containerNode)
                .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException("No data nodes.", "No data nodes provided");
        }
        return dataNodes;

    }
}
