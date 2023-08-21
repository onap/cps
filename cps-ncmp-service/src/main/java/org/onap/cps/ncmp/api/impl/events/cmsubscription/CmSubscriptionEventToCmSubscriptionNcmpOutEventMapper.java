/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.models.CmSubscriptionEvent;
import org.onap.cps.ncmp.api.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.AdditionalInfo;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.AdditionalInfoDetail;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent;
import org.onap.cps.spi.exceptions.DataValidationException;

@Mapper(componentModel = "spring")
public interface CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper {

    @Mapping(source = "cmSubscriptionStatus", target = "data.additionalInfo",
            qualifiedByName = "mapCmSubscriptionStatusToAdditionalInfo")
    CmSubscriptionNcmpOutEvent toCmSubscriptionNcmpOutEvent(CmSubscriptionEvent cmSubscriptionEvent);

    /**
     * Maps list of SubscriptionStatus to an AdditionalInfo.
     *
     * @param cmSubscriptionStatusList containing details
     * @return an AdditionalInfo
     */
    @Named("mapCmSubscriptionStatusToAdditionalInfo")
    default AdditionalInfo mapCmSubscriptionStatusToAdditionalInfo(
            final List<CmSubscriptionStatus> cmSubscriptionStatusList) {
        if (cmSubscriptionStatusList == null || cmSubscriptionStatusList.isEmpty()) {
            throw new DataValidationException("Invalid cmSubscriptionStatusList",
                    "CmSubscriptionStatus list cannot be null or empty");
        }

        final Map<String, List<CmSubscriptionStatus>> rejectedSubscriptionsPerDetails =
                getSubscriptionsPerDetails(cmSubscriptionStatusList, SubscriptionStatus.REJECTED);
        final Map<String, List<String>> rejectedCmHandlesPerDetails =
                getCmHandlesPerDetails(rejectedSubscriptionsPerDetails);
        final List<AdditionalInfoDetail> rejectedCmHandles = getAdditionalInfoDetailList(rejectedCmHandlesPerDetails);

        final Map<String, List<CmSubscriptionStatus>> pendingSubscriptionsPerDetails =
                getSubscriptionsPerDetails(cmSubscriptionStatusList, SubscriptionStatus.PENDING);
        final Map<String, List<String>> pendingCmHandlesPerDetails =
                getCmHandlesPerDetails(pendingSubscriptionsPerDetails);
        final List<AdditionalInfoDetail> pendingCmHandles = getAdditionalInfoDetailList(pendingCmHandlesPerDetails);

        final AdditionalInfo additionalInfo = new AdditionalInfo();
        additionalInfo.setRejected(rejectedCmHandles);
        additionalInfo.setPending(pendingCmHandles);

        return additionalInfo;
    }

    private static Map<String, List<CmSubscriptionStatus>> getSubscriptionsPerDetails(
            final List<CmSubscriptionStatus> cmSubscriptionStatusList, final SubscriptionStatus status) {
        return cmSubscriptionStatusList.stream()
                .filter(subscriptionStatus -> subscriptionStatus.getStatus() == status)
                .collect(Collectors.groupingBy(CmSubscriptionStatus::getDetails));
    }

    private static Map<String, List<String>> getCmHandlesPerDetails(
            final Map<String, List<CmSubscriptionStatus>> cmSubscriptionsPerDetails) {
        return cmSubscriptionsPerDetails.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(CmSubscriptionStatus::getId)
                                .collect(Collectors.toList())
                ));
    }

    private static List<AdditionalInfoDetail> getAdditionalInfoDetailList(
            final Map<String, List<String>> cmHandlesPerDetails) {
        return cmHandlesPerDetails.entrySet().stream()
                .map(entry -> {
                    final AdditionalInfoDetail detail = new AdditionalInfoDetail();
                    detail.setDetails(entry.getKey());
                    detail.setTargets(entry.getValue());
                    return detail;
                }).collect(Collectors.toList());
    }
}
