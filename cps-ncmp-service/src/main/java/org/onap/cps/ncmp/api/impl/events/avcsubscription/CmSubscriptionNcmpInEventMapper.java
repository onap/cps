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
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;

@Mapper(componentModel = "spring")
public interface CmSubscriptionNcmpInEventMapper {

    @Mapping(source = "data.subscription.clientID", target = "clientId")
    @Mapping(source = "data.subscription.name", target = "subscriptionName")
    @Mapping(source = "data.predicates.targets", target = "predicates.targetCmHandles",
            qualifiedByName = "mapTargetsToCmHandleTargets")
    @Mapping(source = "data.predicates.datastore", target = "predicates.datastore")
    YangModelSubscriptionEvent toYangModelSubscriptionEvent(CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent);

    /**
     * Maps list of Targets to list of TargetCmHandle.
     *
     * @param targets list of objects
     * @return TargetCmHandle list
     */
    @Named("mapTargetsToCmHandleTargets")
    default List<YangModelSubscriptionEvent.TargetCmHandle> mapTargetsToCmHandleTargets(List<String> targets) {
        return targets.stream().map(YangModelSubscriptionEvent.TargetCmHandle::new)
                .collect(Collectors.toList());
    }
}
