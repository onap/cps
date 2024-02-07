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

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmSubscriptionPersistenceServiceImpl implements CmSubscriptionPersistenceService {

    private static final String IS_ONGOING_CM_SUBSCRIPTION_CPS_PATH_QUERY = """
            /datastores/datastore[@name='%s']/cm-handles/cm-handle[@id='%s']/filters/filter[@xpath='%s']""";
    private static final List<String> validDatastores =
            Arrays.asList(PASSTHROUGH_RUNNING.getDatastoreName(), PASSTHROUGH_OPERATIONAL.getDatastoreName());

    private final CpsDataService cpsDataService;

    @Override
    public boolean isOngoingCmSubscription(final DatastoreType datastoreType, final String cmHandleId,
            final String xpath) {
        final String isOngoingCmSubscriptionCpsPathQuery =
                IS_ONGOING_CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted(datastoreType.getDatastoreName(), cmHandleId,
                        escapeQuotesByDoublingThem(xpath));
        final Collection<DataNode> existingNodes =
                cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, CM_SUBSCRIPTIONS_ANCHOR_NAME,
                        isOngoingCmSubscriptionCpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS);
        return !existingNodes.isEmpty();
    }

    @Override
    public boolean isValidDataStore(final String incomingDatastore) {
        return validDatastores.contains(incomingDatastore);
    }

    private static String escapeQuotesByDoublingThem(final String inputXpath) {
        return inputXpath.replace("'", "''");
    }
}
