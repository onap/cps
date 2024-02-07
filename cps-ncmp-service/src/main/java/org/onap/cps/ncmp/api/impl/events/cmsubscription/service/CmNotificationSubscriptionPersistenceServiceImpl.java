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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmNotificationSubscriptionPersistenceServiceImpl implements CmNotificationSubscriptionPersistenceService {

    private static final String IS_ONGOING_CM_SUBSCRIPTION_CPS_PATH_QUERY = """
            /datastores/datastore[@name='%s']/cm-handles/cm-handle[@id='%s']/filters/filter[@xpath='%s']""";

    private final CpsQueryService cpsQueryService;

    @Override
    public boolean isOngoingCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
            final String xpath) {
        return !getOngoingCmNotificationSubscriptionIds(datastoreType, cmHandleId, xpath).isEmpty();
    }

    @Override
    public Collection<String> getOngoingCmNotificationSubscriptionIds(final DatastoreType datastoreType,
            final String cmHandleId, final String xpath) {

        final String isOngoingCmSubscriptionCpsPathQuery =
                IS_ONGOING_CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted(datastoreType.getDatastoreName(), cmHandleId,
                        escapeQuotesByDoublingThem(xpath));
        final Collection<DataNode> existingNodes =
                cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_SUBSCRIPTIONS_ANCHOR_NAME,
                        isOngoingCmSubscriptionCpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS);
        if (existingNodes.isEmpty()) {
            return Collections.emptyList();
        }
        return (List<String>) existingNodes.iterator().next().getLeaves().get("subscribers");
    }

    private static String escapeQuotesByDoublingThem(final String inputXpath) {
        return inputXpath.replace("'", "''");
    }
}
