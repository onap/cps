/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import static org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionStatus.PENDING;

import com.hazelcast.map.IMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionCacheObject;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionPredicate;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.ScopeFilter;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmSubscriptionCacheHandler {

    private final IMap<String, Map<String, CmSubscriptionCacheObject>> cmSubscriptionEventCache;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Load Cm Subscription event data in cache.
     *
     * @param cmSubscriptionNcmpInEvent        CM Subscription event
     */
    public void loadCmSubscriptionEventToCache(final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent) {
        final String requestSubscriptionID = cmSubscriptionNcmpInEvent.getData().getSubscriptionId();
        final Map<String, CmSubscriptionCacheObject> dmiToCmSubscriptionCacheObjectMap =
                getAllCmSubscriptionCacheObjectByDmiMap(cmSubscriptionNcmpInEvent);
        cmSubscriptionEventCache.put(requestSubscriptionID, dmiToCmSubscriptionCacheObjectMap);
    }

    /**
     * Get map of Cm Subscription Cache Object by DMI.
     *
     * @param cmSubscriptionNcmpInEvent       CM Subscription event
     * @return                                CmSubscription object by DMI map
     */
    public Map<String, CmSubscriptionCacheObject> getAllCmSubscriptionCacheObjectByDmiMap(
            final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent) {
        final Map<String, CmSubscriptionCacheObject> dmiToCmSubscriptionCacheObjectMap = new HashMap<>();
        final List<Predicate> cmSubscriptionRequestPredicates = cmSubscriptionNcmpInEvent.getData().getPredicates();

        for (final Predicate requestPredicate : cmSubscriptionRequestPredicates) {
            final Map<String, List<String>> targetCmHandlesByDmiMap =
                    groupTargetCmHandlesByDmi(requestPredicate);
            for (final Map.Entry<String, List<String>> targetCmHandlesByDmi: targetCmHandlesByDmiMap.entrySet()) {
                final String dmiServiceName = targetCmHandlesByDmi.getKey();
                final List<String> targetCmHandles = targetCmHandlesByDmi.getValue();
                final CmSubscriptionCacheObject cmSubscriptionCacheObject =
                        createCmSubscriptionCacheObject(requestPredicate);
                cmSubscriptionCacheObject.getCmSubscriptionPredicates().get(0)
                        .setTargetFilter(targetCmHandles);
                dmiToCmSubscriptionCacheObjectMap.put(dmiServiceName, cmSubscriptionCacheObject);
            }
        }
        return dmiToCmSubscriptionCacheObjectMap;
    }

    protected Map<String, List<String>> groupTargetCmHandlesByDmi(final Predicate requestPredicate) {
        final List<String> targetCmHandles = requestPredicate.getTargetFilter();
        final Map<String, List<String>> targetCmHandlesByDmiServiceNames = new HashMap<>();
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(targetCmHandles);

        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final String dmiServiceName = yangModelCmHandle.getDmiServiceName();
            final String cmHandleId = yangModelCmHandle.getId();
            targetCmHandlesByDmiServiceNames.computeIfAbsent(dmiServiceName, k -> new ArrayList<>()).add(cmHandleId);
        }
        return targetCmHandlesByDmiServiceNames;
    }

    private CmSubscriptionCacheObject createCmSubscriptionCacheObject(final Predicate requestPredicate) {
        final CmSubscriptionCacheObject cmSubscriptionCacheObject = new CmSubscriptionCacheObject();
        final CmSubscriptionPredicate cmSubscriptionPredicate = new CmSubscriptionPredicate();
        final ScopeFilter scopeFilter = new ScopeFilter();

        cmSubscriptionCacheObject.setCmSubscriptionPredicates(new ArrayList<>());
        cmSubscriptionCacheObject.setCmSubscriptionStatus(PENDING);
        scopeFilter.setXpathFilters(requestPredicate.getScopeFilter().getXpathFilter());
        scopeFilter.setDatastoreType((DatastoreType.fromDatastoreName(
                requestPredicate.getScopeFilter().getDatastore().toString())));
        cmSubscriptionPredicate.setScopeFilter(scopeFilter);
        cmSubscriptionCacheObject.getCmSubscriptionPredicates().add(cmSubscriptionPredicate);

        return cmSubscriptionCacheObject;
    }
}