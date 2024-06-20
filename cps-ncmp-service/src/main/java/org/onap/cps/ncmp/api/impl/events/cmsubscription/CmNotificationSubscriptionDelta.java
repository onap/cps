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
import java.util.Collection;
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
public class CmNotificationSubscriptionDelta {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;

    /**
     * Get the delta for a given predicates list.
     *
     * @param dmiCmNotificationSubscriptionPredicates list of DmiCmNotificationSubscriptionPredicates
     * @return delta list of DmiCmNotificationSubscriptionPredicates
     */
    public List<DmiCmNotificationSubscriptionPredicate> getDelta(
        final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates) {
        final List<DmiCmNotificationSubscriptionPredicate> delta = new ArrayList<>();

        for (final DmiCmNotificationSubscriptionPredicate cmNotificationSubscriptionPredicate:
            dmiCmNotificationSubscriptionPredicates) {

            final Set<String> targetCmHandleIds = new HashSet<>();
            final Set<String> xpaths = new HashSet<>();
            final DatastoreType datastoreType = cmNotificationSubscriptionPredicate.getDatastoreType();

            for (final String cmHandleId : cmNotificationSubscriptionPredicate.getTargetCmHandleIds()) {
                for (final String xpath : cmNotificationSubscriptionPredicate.getXpaths()) {
                    if (!cmNotificationSubscriptionPersistenceService.isOngoingCmNotificationSubscription(datastoreType,
                            cmHandleId, xpath)) {
                        xpaths.add(xpath);
                        targetCmHandleIds.add(cmHandleId);

                    }
                }
            }

            populateValidDmiCmNotificationSubscriptionPredicateDelta(targetCmHandleIds, xpaths, datastoreType, delta);
        }
        return delta;
    }

    /**
     * Get the delta for a given predicates list with shared subscriptions.
     *
     * @param dmiCmNotificationSubscriptionPredicates list of DmiCmNotificationSubscriptionPredicates
     * @return delta list of DmiCmNotificationSubscriptionPredicates
     */
    public List<DmiCmNotificationSubscriptionPredicate> getPredicatesUsedOnlyBySubscriptionId(
            final String subscriptionId,
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates) {
        final List<DmiCmNotificationSubscriptionPredicate> delta = new ArrayList<>();
        for (final DmiCmNotificationSubscriptionPredicate cmNotificationSubscriptionPredicate:
                dmiCmNotificationSubscriptionPredicates) {

            final Set<String> targetCmHandleIds = new HashSet<>();
            final Set<String> xpaths = new HashSet<>();
            final DatastoreType datastoreType = cmNotificationSubscriptionPredicate.getDatastoreType();

            for (final String cmHandleId : cmNotificationSubscriptionPredicate.getTargetCmHandleIds()) {
                for (final String xpath : cmNotificationSubscriptionPredicate.getXpaths()) {
                    final Collection<String> ongoingCmNotificationSubscriptionIds =
                            cmNotificationSubscriptionPersistenceService.getOngoingCmNotificationSubscriptionIds(
                                    datastoreType, cmHandleId, xpath);
                    if (ongoingCmNotificationSubscriptionIds.size() == 1
                            && ongoingCmNotificationSubscriptionIds.contains(subscriptionId)) {
                        xpaths.add(xpath);
                        targetCmHandleIds.add(cmHandleId);
                    }
                }
            }
            populateValidDmiCmNotificationSubscriptionPredicateDelta(targetCmHandleIds, xpaths, datastoreType, delta);
        }
        return delta;
    }

    private void populateValidDmiCmNotificationSubscriptionPredicateDelta(final Set<String> targetCmHandleIds,
            final Set<String> xpaths, final DatastoreType datastoreType,
            final List<DmiCmNotificationSubscriptionPredicate> delta) {
        if (!(targetCmHandleIds.isEmpty() || xpaths.isEmpty())) {
            final DmiCmNotificationSubscriptionPredicate predicateDelta =
                    new DmiCmNotificationSubscriptionPredicate(targetCmHandleIds, datastoreType, xpaths);
            delta.add(predicateDelta);
        }
    }

}
