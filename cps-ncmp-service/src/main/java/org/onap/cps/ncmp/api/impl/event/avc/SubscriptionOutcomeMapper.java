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

package org.onap.cps.ncmp.api.impl.event.avc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.onap.cps.ncmp.events.avc.subscription.v1.SubscriptionEventOutcome;

@Mapper(componentModel = "spring")
public interface SubscriptionOutcomeMapper {

    @Mapping(source = "clientId", target = "event.subscription.clientID")
    @Mapping(source = "subscriptionName", target = "event.subscription.name")
    @Mapping(source = "cmHandleIdToStatus", target = "event.predicates.rejectedTargets",
            qualifiedByName = "mapStatusToCmHandleRejected")
    @Mapping(source = "cmHandleIdToStatus", target = "event.predicates.acceptedTargets",
            qualifiedByName = "mapStatusToCmHandleAccepted")
    @Mapping(source = "cmHandleIdToStatus", target = "event.predicates.pendingTargets",
            qualifiedByName = "mapStatusToCmHandlePending")
    SubscriptionEventOutcome toSubscriptionEventOutcome(
            SubscriptionEventResponse subscriptionEventResponse);

    /**
     * Maps StatusToCMHandle to list of TargetCmHandle rejected.
     *
     * @param targets as a map
     * @return TargetCmHandle list
     */
    @Named("mapStatusToCmHandleRejected")
    default List<Object> mapStatusToCmHandleRejected(Map<String, SubscriptionStatus> targets) {
        return targets.entrySet()
                .stream().filter(target -> SubscriptionStatus.REJECTED.equals(target.getValue()))
                .map(target -> target.getKey())
                .collect(Collectors.toList());
    }

    /**
     * Maps StatusToCMHandle to list of TargetCmHandle accepted.
     *
     * @param targets as a map
     * @return TargetCmHandle list
     */
    @Named("mapStatusToCmHandleAccepted")
    default List<Object> mapStatusToCmHandleAccepted(Map<String, SubscriptionStatus> targets) {
        return targets.entrySet()
                .stream().filter(target -> SubscriptionStatus.ACCEPTED.equals(target.getValue()))
                .map(target -> target.getKey())
                .collect(Collectors.toList());
    }

    /**
     * Maps StatusToCMHandle to list of TargetCmHandle pending.
     *
     * @param targets as a map
     * @return TargetCmHandle list
     */
    @Named("mapStatusToCmHandlePending")
    default List<Object> mapStatusToCmHandlePending(Map<String, SubscriptionStatus> targets) {
        return targets.entrySet()
                .stream().filter(target -> SubscriptionStatus.PENDING.equals(target.getValue()))
                .map(target -> target.getKey())
                .collect(Collectors.toList());
    }
}
