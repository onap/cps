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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmSubscriptionComparator {

    private final CmSubscriptionPersistenceService cmSubscriptionPersistenceService;

    /**
     * Get the new Dmi Predicates for a given predicates list.
     *
     * @param existingDmiCmSubscriptionPredicates list of DmiCmNotificationSubscriptionPredicates
     * @return new list of DmiCmNotificationSubscriptionPredicates
     */
    public List<DmiCmSubscriptionPredicate> getNewDmiSubscriptionPredicates(
            final List<DmiCmSubscriptionPredicate> existingDmiCmSubscriptionPredicates) {
        final List<DmiCmSubscriptionPredicate> newDmiCmSubscriptionPredicates =
                new ArrayList<>();
        for (final DmiCmSubscriptionPredicate dmiCmSubscriptionPredicate : existingDmiCmSubscriptionPredicates) {
            final Set<String> targetCmHandleIds = new HashSet<>();
            final Set<String> xpaths = new HashSet<>();
            final DatastoreType datastoreType = dmiCmSubscriptionPredicate.getDatastoreType();
            for (final String cmHandleId : dmiCmSubscriptionPredicate.getTargetCmHandleIds()) {
                for (final String xpath : dmiCmSubscriptionPredicate.getXpaths()) {
                    if (!cmSubscriptionPersistenceService.isOngoingCmSubscription(datastoreType,
                            cmHandleId, xpath)) {
                        xpaths.add(xpath);
                        targetCmHandleIds.add(cmHandleId);

                    }
                }
            }
            populateValidDmiSubscriptionPredicates(targetCmHandleIds, xpaths, datastoreType,
                    newDmiCmSubscriptionPredicates);
        }
        return newDmiCmSubscriptionPredicates;
    }

    private void populateValidDmiSubscriptionPredicates(final Set<String> targetCmHandleIds,
            final Set<String> xpaths, final DatastoreType datastoreType,
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {
        if (!targetCmHandleIds.isEmpty()) {
            final DmiCmSubscriptionPredicate dmiCmSubscriptionPredicate =
                    new DmiCmSubscriptionPredicate(targetCmHandleIds, datastoreType, xpaths);
            dmiCmSubscriptionPredicates.add(dmiCmSubscriptionPredicate);
        }
    }

}
