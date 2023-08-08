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
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus;

@Mapper(componentModel = "spring")
public interface CmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper {

    @Mapping(source = "data.clientId", target = "clientId")
    @Mapping(source = "data.subscriptionName", target = "subscriptionName")
    @Mapping(source = "data.subscriptionStatus", target = "predicates.targetCmHandles",
            qualifiedByName = "mapSubscriptionStatusToCmHandleTargets")
    YangModelSubscriptionEvent toYangModelSubscriptionEvent(
            CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent);

    /**
     * Maps SubscriptionStatus to list of TargetCmHandle.
     *
     * @param subscriptionStatus as a list
     * @return TargetCmHandle list
     */
    @Named("mapSubscriptionStatusToCmHandleTargets")
    default List<YangModelSubscriptionEvent.TargetCmHandle> mapSubscriptionStatusToCmHandleTargets(
            List<SubscriptionStatus> subscriptionStatus) {
        return subscriptionStatus.stream().map(status -> new YangModelSubscriptionEvent.TargetCmHandle(status.getId(),
                org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus.fromString(status.getStatus().value()),
                        status.getDetails())).collect(Collectors.toList());
    }
}
