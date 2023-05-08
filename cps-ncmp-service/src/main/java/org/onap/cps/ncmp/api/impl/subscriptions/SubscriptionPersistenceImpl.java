/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.subscriptions;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;

import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubscriptionPersistenceImpl implements SubscriptionPersistence {

    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_PARENT = "/subscription-registry";

    private final JsonObjectMapper jsonObjectMapper;
    private final CpsDataService cpsDataService;

    @Override
    public void saveSubscriptionEvent(final YangModelSubscriptionEvent yangModelSubscriptionEvent) {
        final String subscriptionEventJsonData =
                createSubscriptionEventJsonData(jsonObjectMapper.asJsonString(yangModelSubscriptionEvent));
        final Collection<DataNode> dataNodes = cpsDataService.getDataNodes(SUBSCRIPTION_DATASPACE_NAME,
                SUBSCRIPTION_ANCHOR_NAME, SUBSCRIPTION_REGISTRY_PARENT, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        final Optional<DataNode> dataNodeFirst = dataNodes.stream().findFirst();
        final boolean isCreateOperation =
                dataNodeFirst.isPresent() && dataNodeFirst.get().getChildDataNodes().isEmpty();
        saveOrUpdateSubscriptionEventYangModel(subscriptionEventJsonData, isCreateOperation);
    }

    private void saveOrUpdateSubscriptionEventYangModel(final String subscriptionEventJsonData,
                                                        final boolean isCreateOperation) {
        if (isCreateOperation) {
            log.info("SubscriptionEventJsonData to be saved into DB {}", subscriptionEventJsonData);
            cpsDataService.saveListElements(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                    SUBSCRIPTION_REGISTRY_PARENT, subscriptionEventJsonData, NO_TIMESTAMP);
        } else {
            log.info("SubscriptionEventJsonData to be updated into DB {}", subscriptionEventJsonData);
            cpsDataService.updateDataNodeAndDescendants(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                    SUBSCRIPTION_REGISTRY_PARENT, subscriptionEventJsonData, NO_TIMESTAMP);
        }
    }

    @Override
    public Collection<DataNode> getDataNodesForSubscriptionEvent() {
        return cpsDataService.getDataNodes(SUBSCRIPTION_DATASPACE_NAME,
                SUBSCRIPTION_ANCHOR_NAME, SUBSCRIPTION_REGISTRY_PARENT,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

    private static String createSubscriptionEventJsonData(final String yangModelSubscriptionAsJson) {
        return "{\"subscription\":[" + yangModelSubscriptionAsJson + "]}";
    }
}
