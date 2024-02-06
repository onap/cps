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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.service.CmNotificationSubscriptionPersistenceService;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmSubscriptionDelta {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;

    /**
     * Get the delta for a given predicates list.
     *
     * @param cmNotificationSubscriptionPredicates list of cmSubscriptionPredicates
     * @return delta list of cmSubscriptionPredicates
     */
    public List<DmiCmNotificationSubscriptionPredicate> getDelta(
        final List<DmiCmNotificationSubscriptionPredicate> cmNotificationSubscriptionPredicates) {
        final List<DmiCmNotificationSubscriptionPredicate> delta = new ArrayList<>();

        for (final DmiCmNotificationSubscriptionPredicate cmNotificationSubscriptionPredicate:
            cmNotificationSubscriptionPredicates) {

            final Set<String> targetFilters = new HashSet<>();
            final Set<String> xpathFilters = new HashSet<>();
            final DatastoreType datastoreType = cmNotificationSubscriptionPredicate.getDatastoreType();

            for (final String cmHandleId : cmNotificationSubscriptionPredicate.getTargetCmHandleIds()) {
                for (final String xpath : cmNotificationSubscriptionPredicate.getXpaths()) {
                    if (!cmNotificationSubscriptionPersistenceService.isOngoingCmNotificationSubscription(datastoreType,
                        cmHandleId, xpath)) {
                        xpathFilters.add(xpath);
                        targetFilters.add(cmHandleId);

                    }
                }
            }

            final DmiCmNotificationSubscriptionPredicate predicateDelta =
                new DmiCmNotificationSubscriptionPredicate(targetFilters, datastoreType, xpathFilters);

            delta.add(predicateDelta);
        }
        return delta;
    }

}
