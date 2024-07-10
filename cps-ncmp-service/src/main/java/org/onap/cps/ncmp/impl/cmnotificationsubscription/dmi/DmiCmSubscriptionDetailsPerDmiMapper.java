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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionKey;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;

public class DmiCmSubscriptionDetailsPerDmiMapper {

    /**
     * Mapper to group the incoming dmi subscription keys per dmi plugin.
     *
     * @param subscribersPerDmi details managed by each dmi plugin
     * @return Grouped Dmi Subscription details per dmi plugin
     */
    public Map<String, DmiCmSubscriptionDetails> toDmiCmSubscriptionsPerDmi(
            final Map<String, Collection<DmiCmSubscriptionKey>> subscribersPerDmi) {

        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi = new HashMap<>();

        subscribersPerDmi.forEach((dmiPluginName, dmiCmSubscriptionKeys) -> {
            final Map<DatastoreTypeXpathCompoundKey, List<DmiCmSubscriptionKey>> groupedByDatastoreTypeAndXpath =
                    dmiCmSubscriptionKeys.stream().collect(Collectors.groupingBy(
                            datastoreTypeAndXpath -> new DatastoreTypeXpathCompoundKey(
                                    fromDatastoreName(datastoreTypeAndXpath.datastoreName()),
                                    datastoreTypeAndXpath.xpath())));

            final List<DmiCmSubscriptionPredicate> dmiSubscriptionPredicates =
                    getDmiCmSubscriptionPredicates(groupedByDatastoreTypeAndXpath);

            final DmiCmSubscriptionDetails dmiCmSubscriptionDetails =
                    new DmiCmSubscriptionDetails(dmiSubscriptionPredicates, PENDING);

            dmiSubscriptionsPerDmi.put(dmiPluginName, dmiCmSubscriptionDetails);
        });

        return dmiSubscriptionsPerDmi;
    }

    private static List<DmiCmSubscriptionPredicate> getDmiCmSubscriptionPredicates(
            final Map<DatastoreTypeXpathCompoundKey, List<DmiCmSubscriptionKey>> groupedByDatastoreTypeAndXpath) {
        return groupedByDatastoreTypeAndXpath.entrySet().stream().map(datastoreTypeXpathGroupEntry -> {
            final DatastoreTypeXpathCompoundKey compoundKey = datastoreTypeXpathGroupEntry.getKey();
            final Set<String> cmHandleIds =
                    datastoreTypeXpathGroupEntry.getValue().stream().map(DmiCmSubscriptionKey::cmHandleId)
                            .collect(Collectors.toSet());
            final Set<String> xpaths = Collections.singleton(compoundKey.xpath());
            return new DmiCmSubscriptionPredicate(cmHandleIds, compoundKey.datastoreType(), xpaths);
        }).toList();
    }

    private record DatastoreTypeXpathCompoundKey(DatastoreType datastoreType, String xpath) { }

}

