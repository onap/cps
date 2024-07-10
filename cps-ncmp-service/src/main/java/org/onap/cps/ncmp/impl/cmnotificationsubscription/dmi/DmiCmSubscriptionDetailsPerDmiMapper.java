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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi;

import static org.onap.cps.ncmp.api.data.models.DatastoreType.fromDatastoreName;
import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.PENDING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionKey;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.springframework.stereotype.Component;

@Component
public class DmiCmSubscriptionDetailsPerDmiMapper {

    /**
     * Maps Dmi Subscription Keys grouped by Dmi Plugin to DmiCmSubscriptionDetails per Dmi plugin.
     *
     * @param subscribersPerDmi Details managed by each dmi plugin
     * @return Grouped Dmi Subscription details per dmi plugin
     */
    public Map<String, DmiCmSubscriptionDetails> toDmiCmSubscriptionsPerDmi(
            final Map<String, Collection<DmiCmSubscriptionKey>> subscribersPerDmi) {

        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi = new HashMap<>();

        subscribersPerDmi.forEach((dmiPluginName, dmiCmSubscriptionKeys) -> {
            final Map<DatastoreTypeAndXpath, List<DmiCmSubscriptionKey>> groupedByDatastoreTypeAndXpath =
                    groupByDatastoreTypeAndXpath(dmiCmSubscriptionKeys);

            final List<DmiCmSubscriptionPredicate> dmiSubscriptionPredicates =
                    createDmiCmSubscriptionPredicates(groupedByDatastoreTypeAndXpath);

            final DmiCmSubscriptionDetails dmiCmSubscriptionDetails =
                    new DmiCmSubscriptionDetails(dmiSubscriptionPredicates, PENDING);

            dmiSubscriptionsPerDmi.put(dmiPluginName, dmiCmSubscriptionDetails);
        });

        return dmiSubscriptionsPerDmi;
    }

    private static Map<DatastoreTypeAndXpath, List<DmiCmSubscriptionKey>> groupByDatastoreTypeAndXpath(
            final Collection<DmiCmSubscriptionKey> dmiCmSubscriptionKeys) {
        return dmiCmSubscriptionKeys.stream().collect(Collectors.groupingBy(
                datastoreTypeAndXpath -> new DatastoreTypeAndXpath(
                        fromDatastoreName(datastoreTypeAndXpath.datastoreName()), datastoreTypeAndXpath.xpath())));
    }

    private static List<DmiCmSubscriptionPredicate> createDmiCmSubscriptionPredicates(
            final Map<DatastoreTypeAndXpath, List<DmiCmSubscriptionKey>> groupedByDatastoreTypeAndXpath) {
        final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates = new ArrayList<>();

        for (final Map.Entry<DatastoreTypeAndXpath, List<DmiCmSubscriptionKey>> datastoreTypeXpathGroupEntry :
                groupedByDatastoreTypeAndXpath.entrySet()) {
            final DatastoreTypeAndXpath datastoreTypeAndXpath = datastoreTypeXpathGroupEntry.getKey();
            final Set<String> cmHandleIds = new HashSet<>();

            for (final DmiCmSubscriptionKey dmiCmSubscriptionKey : datastoreTypeXpathGroupEntry.getValue()) {
                cmHandleIds.add(dmiCmSubscriptionKey.cmHandleId());
            }

            final Set<String> xpaths = Collections.singleton(datastoreTypeAndXpath.xpath());
            dmiCmSubscriptionPredicates.add(
                    new DmiCmSubscriptionPredicate(cmHandleIds, datastoreTypeAndXpath.datastoreType(), xpaths));
        }

        return dmiCmSubscriptionPredicates;
    }


    private record DatastoreTypeAndXpath(DatastoreType datastoreType, String xpath) { }

}

