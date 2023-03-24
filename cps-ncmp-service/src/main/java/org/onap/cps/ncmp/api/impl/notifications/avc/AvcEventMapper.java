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

package org.onap.cps.ncmp.api.impl.notifications.avc;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.event.model.AvcEvent;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;


/**
 * Mapper for converting incoming {@link AvcEvent} to outgoing {@link AvcEvent}.
 */
@Mapper(componentModel = "spring")
public interface AvcEventMapper {

    @Mapping(source = "eventId", target = "eventId", qualifiedByName = "avcEventId")
    AvcEvent toOutgoingAvcEvent(AvcEvent incomingAvcEvent);

    @Named("avcEventId")
    static String getAvcEventId(String eventId) {
        return UUID.randomUUID().toString();
    }

    @Mapping(source = "event.subscription.clientID", target = "clientId")
    @Mapping(source = "event.subscription.name", target = "subscriptionName")
    @Mapping(source = "event.subscription.isTagged", target = "tagged", qualifiedByName = "mapIsTagged")
    @Mapping(source = "event.predicates.targets",
        target = "predicates.targetCmHandles", qualifiedByName = "mapTargetsToCmHandleTargets")
    @Mapping(source = "event.predicates.datastore", target = "predicates.datastore")
    YangModelSubscriptionEvent toYangModelSubscriptionEvent(SubscriptionEvent subscriptionEvent);

    @Named("mapTargetsToCmHandleTargets")
    default List<YangModelSubscriptionEvent.TargetCmHandle> mapTargetsToCmHandleTargets(List<Object> targets) {
        return targets.stream().map(
            target -> new YangModelSubscriptionEvent.TargetCmHandle(target.toString())).collect(Collectors.toList());
    }

    @Named("mapIsTagged")
    default boolean mapIsTagged(Boolean isTagged) {
        return (isTagged == null) ? false : isTagged;
    }
}
