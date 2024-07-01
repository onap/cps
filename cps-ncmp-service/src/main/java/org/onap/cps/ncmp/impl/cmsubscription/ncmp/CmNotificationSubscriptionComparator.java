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

package org.onap.cps.ncmp.impl.cmsubscription.ncmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmsubscription.utils.CmNotificationSubscriptionPersistenceService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmNotificationSubscriptionComparator {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;

    /**
     * Get the new Dmi Predicates for a given predicates list.
     *
     * @param dmiCmNotificationSubscriptionPredicates list of DmiCmNotificationSubscriptionPredicates
     * @return new list of DmiCmNotificationSubscriptionPredicates
     */
    public List<DmiCmNotificationSubscriptionPredicate> getNewDmiCmNotificationSubscriptionPredicates(
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates) {
        final List<DmiCmNotificationSubscriptionPredicate> newDmiCmNotificationSubscriptionPredicates =
                new ArrayList<>();

        for (final DmiCmNotificationSubscriptionPredicate cmNotificationSubscriptionPredicate :
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

            populateValidDmiCmNotificationSubscriptionPredicateDelta(targetCmHandleIds, xpaths, datastoreType,
                    newDmiCmNotificationSubscriptionPredicates);
        }
        return newDmiCmNotificationSubscriptionPredicates;
    }

    private void populateValidDmiCmNotificationSubscriptionPredicateDelta(final Set<String> targetCmHandleIds,
            final Set<String> xpaths, final DatastoreType datastoreType,
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates) {
        if (!(targetCmHandleIds.isEmpty() || xpaths.isEmpty())) {
            final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate =
                    new DmiCmNotificationSubscriptionPredicate(targetCmHandleIds, datastoreType, xpaths);
            dmiCmNotificationSubscriptionPredicates.add(dmiCmNotificationSubscriptionPredicate);
        }
    }

}
