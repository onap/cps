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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmDataSubscriptionEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.Predicate;

@Mapper(componentModel = "spring")
public interface CmSubscriptionNcmpInEventMapper {

    @Mapping(source = "data.subscriptionId", target = "name")
    @Mapping(source = "data.predicates", target = "cmHandles", qualifiedByName = "mapPredicatesToCmHandles")
    YangModelCmDataSubscriptionEvent toYangModelCmDataSubscriptionEvent(
        CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent);

    /**
     * Maps Predicates to list of CmHandles.
     *
     * @param predicates as a list
     * @return CmHandle list
     */
    @Named("mapPredicatesToCmHandles")
    default List<YangModelCmDataSubscriptionEvent.CmHandle> mapPredicatesToCmHandles(
        List<Predicate> predicates) {
        if (predicates == null) {
            return Collections.emptyList();
        }
        final List<YangModelCmDataSubscriptionEvent.CmHandle> cmHandles = new ArrayList<>();
        for (final Predicate predicate: predicates) {
            final List<YangModelCmDataSubscriptionEvent.Filter> filters = new ArrayList<>();
            filters.add(new YangModelCmDataSubscriptionEvent.Filter(
                predicate.getScopeFilter().getDatastore().value(),
                predicate.getScopeFilter().getXpathFilter()));

            for (final String cmhandleId : predicate.getTargetFilter()) {
                final YangModelCmDataSubscriptionEvent.CmHandle cmHandle =
                    new YangModelCmDataSubscriptionEvent.CmHandle(cmhandleId, filters);
                cmHandles.add(cmHandle);
            }
        }
        return cmHandles;
    }

}
