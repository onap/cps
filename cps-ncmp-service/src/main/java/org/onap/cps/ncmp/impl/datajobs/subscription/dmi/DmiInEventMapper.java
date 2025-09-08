/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.dmi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.CmHandle;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.Data;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataSelector;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.ProductionJobDefinition;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.TargetSelector;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.utils.JexParser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiInEventMapper {

    private final InventoryPersistence inventoryPersistence;

    /**
     * This method maps relevant details for a subscription to a data job subscription DMI in event.
     *
     * @param cmHandleIds           list of cm handle ID(s)
     * @param dataNodeSelectors     list of data node selectors
     * @param notificationTypes     the list of notification types
     * @param notificationFilter    the notification filter
     * @return                      data job subscription DMI in event
     */
    public DataJobSubscriptionDmiInEvent toDmiInEvent(final List<String> cmHandleIds,
                                                      final List<String> dataNodeSelectors,
                                                      final List<String> notificationTypes,
                                                      final String notificationFilter) {
        final DataJobSubscriptionDmiInEvent dmiInEvent = new DataJobSubscriptionDmiInEvent();
        final Data data = new Data();
        final String dataNodeSelector = JexParser.toJsonExpressionsAsString(dataNodeSelectors);
        data.setCmHandles(mapToCmSubscriptionCmHandleWithAdditionalProperties(new HashSet<>(cmHandleIds)));
        addProductJobDefinition(data, dataNodeSelector);
        addDataSelector(data, notificationTypes, notificationFilter);
        dmiInEvent.setData(data);
        return dmiInEvent;
    }

    private static void addProductJobDefinition(final Data data, final String dataNodeSelector) {
        data.setProductionJobDefinition(new ProductionJobDefinition());
        data.getProductionJobDefinition().setTargetSelector(new TargetSelector());
        data.getProductionJobDefinition().getTargetSelector().setDataNodeSelector(dataNodeSelector);
    }

    private static void addDataSelector(final Data data, final List<String> notificationTypes,
                                        final String notificationFilter) {
        data.getProductionJobDefinition().setDataSelector(new DataSelector());
        data.getProductionJobDefinition().getDataSelector().setNotificationTypes(notificationTypes);
        data.getProductionJobDefinition().getDataSelector().setNotificationFilter(notificationFilter);
    }

    private List<CmHandle> mapToCmSubscriptionCmHandleWithAdditionalProperties(final Set<String> cmHandleIds) {

        final List<CmHandle> cmSubscriptionCmHandles = new ArrayList<>();

        inventoryPersistence.getYangModelCmHandles(cmHandleIds).forEach(yangModelCmHandle -> {
            final CmHandle cmHandle = new CmHandle();
            final Map<String, String> cmHandleAdditionalProperties = new LinkedHashMap<>();
            yangModelCmHandle.getAdditionalProperties()
                .forEach(additionalProperty -> cmHandleAdditionalProperties.put(additionalProperty.name(),
                    additionalProperty.value()));
            cmHandle.setCmhandleId(yangModelCmHandle.getId());
            cmHandle.setPrivateProperties(cmHandleAdditionalProperties);
            cmSubscriptionCmHandles.add(cmHandle);
        });

        return cmSubscriptionCmHandles;

    }

}
