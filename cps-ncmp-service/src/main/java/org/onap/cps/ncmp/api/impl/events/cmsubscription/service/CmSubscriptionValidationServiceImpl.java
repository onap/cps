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
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CmSubscriptionValidationServiceImpl implements CmSubscriptionValidationService {

    private static final List<String> validDatastores =
            Arrays.asList(PASSTHROUGH_RUNNING.getDatastoreName(), PASSTHROUGH_OPERATIONAL.getDatastoreName());


    @Override
    public boolean isValidDataStore(final String incomingDatastore) {
        return validDatastores.contains(incomingDatastore);
    }


}
