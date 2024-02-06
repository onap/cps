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

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmSubscriptionValidationServiceImpl implements CmSubscriptionValidationService {

    private final CpsQueryService cpsQueryService;
    private static final String SUBSCRIPTION_ANCHOR_NAME = "cm-data-subscriptions";
    private static final List<String> validDatastores =
            Arrays.asList(PASSTHROUGH_RUNNING.getDatastoreName(), PASSTHROUGH_OPERATIONAL.getDatastoreName());
    private static final String IS_SUBSCRIPTION_ID_VALID_CPS_PATH_QUERY = """
            //filter/subscribers[text()='%s']""";

    @Override
    public boolean isValidSubscriptionId(final String subscriptionId) {
        final String isSubscriptionIdValidCpsPathQuery =
                IS_SUBSCRIPTION_ID_VALID_CPS_PATH_QUERY.formatted(subscriptionId);

        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                isSubscriptionIdValidCpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS).isEmpty();
    }

    @Override
    public boolean isValidDataStore(final String incomingDatastore) {
        return validDatastores.contains(incomingDatastore);
    }
}
