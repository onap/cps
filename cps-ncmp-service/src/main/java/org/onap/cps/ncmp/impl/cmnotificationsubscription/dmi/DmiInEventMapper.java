/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.CmHandle;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.Data;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.Predicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.ScopeFilter;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiInEventMapper {

    private final InventoryPersistence inventoryPersistence;

    /**
     * Mapper to form a request for the DMI Plugin for the Cm Notification Subscription.
     *
     * @param dmiCmSubscriptionPredicates Collection of Cm Notification Subscription predicates
     * @return DmiInEvent to be sent to DMI Plugin
     */
    public DmiInEvent toDmiInEvent(final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {
        final DmiInEvent dmiInEvent = new DmiInEvent();
        final Data cmSubscriptionData = new Data();
        cmSubscriptionData.setPredicates(mapToDmiInEventPredicates(dmiCmSubscriptionPredicates));
        cmSubscriptionData.setCmHandles(mapToCmSubscriptionCmHandleWithPrivateProperties(
                extractUniqueCmHandleIds(dmiCmSubscriptionPredicates)));
        dmiInEvent.setData(cmSubscriptionData);
        return dmiInEvent;

    }

    private List<Predicate> mapToDmiInEventPredicates(
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {

        final List<Predicate> predicates = new ArrayList<>();

        dmiCmSubscriptionPredicates.forEach(dmiCmNotificationSubscriptionPredicate -> {
            final Predicate predicate = new Predicate();
            final ScopeFilter scopeFilter = new ScopeFilter();
            scopeFilter.setDatastore(ScopeFilter.Datastore.fromValue(
                    dmiCmNotificationSubscriptionPredicate.getDatastoreType().getDatastoreName()));
            scopeFilter.setXpathFilter(dmiCmNotificationSubscriptionPredicate.getXpaths().stream().toList());
            predicate.setScopeFilter(scopeFilter);
            predicate.setTargetFilter(dmiCmNotificationSubscriptionPredicate.getTargetCmHandleIds().stream().toList());
            predicates.add(predicate);
        });

        return predicates;

    }

    private List<CmHandle> mapToCmSubscriptionCmHandleWithPrivateProperties(final Set<String> cmHandleIds) {

        final List<CmHandle> cmSubscriptionCmHandles = new ArrayList<>();

        inventoryPersistence.getYangModelCmHandles(cmHandleIds).forEach(yangModelCmHandle -> {
            final CmHandle cmhandle = new CmHandle();
            final Map<String, String> cmhandleDmiProperties = new LinkedHashMap<>();
            yangModelCmHandle.getDmiProperties()
                    .forEach(dmiProperty -> cmhandleDmiProperties.put(dmiProperty.getName(), dmiProperty.getValue()));
            cmhandle.setCmhandleId(yangModelCmHandle.getId());
            cmhandle.setPrivateProperties(cmhandleDmiProperties);
            cmSubscriptionCmHandles.add(cmhandle);
        });

        return cmSubscriptionCmHandles;

    }

    private Set<String> extractUniqueCmHandleIds(final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {

        final Set<String> cmHandleIds = new HashSet<>();
        dmiCmSubscriptionPredicates.forEach(dmiCmNotificationSubscriptionPredicate -> cmHandleIds.addAll(
                dmiCmNotificationSubscriptionPredicate.getTargetCmHandleIds()));
        return cmHandleIds;
    }


}
