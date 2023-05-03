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

package org.onap.cps.ncmp.api.impl.events.avcsubscription;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;

@Mapper(componentModel = "spring")
public interface SubscriptionEventResponseMapper {

    @Mapping(source = "clientId", target = "clientId")
    @Mapping(source = "subscriptionName", target = "subscriptionName")
    @Mapping(source = "cmHandleIdToStatus", target = "predicates.targetCmHandles",
            qualifiedByName = "mapStatusToCmHandleTargets")
    YangModelSubscriptionEvent toYangModelSubscriptionEvent(
            SubscriptionEventResponse subscriptionEventResponse);

    /**
     * Maps StatusToCMHandle to list of TargetCmHandle.
     *
     * @param targets as a map
     * @return TargetCmHandle list
     */
    @Named("mapStatusToCmHandleTargets")
    default List<YangModelSubscriptionEvent.TargetCmHandle> mapStatusToCmHandleTargets(
            Map<String, SubscriptionStatus> targets) {
        return targets.entrySet().stream().map(target ->
                new YangModelSubscriptionEvent.TargetCmHandle(target.getKey(), target.getValue())).collect(
                Collectors.toList());
    }
}
