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

import org.onap.cps.ncmp.api.impl.operations.DatastoreType;

public interface CmSubscriptionService {

    String NCMP_DATASPACE_NAME = "NCMP-Admin";
    String CM_SUBSCRIPTIONS_ANCHOR_NAME = "cm-data-subscriptions";

    /**
     * Check if we have an ongoing cm subscription based on the parameters.
     *
     * @param datastoreType valid datastore type
     * @param cmHandleId    cmhandle id
     * @param xpath         valid xpath
     * @return true for ongoing cmsubscription , otherwise false
     */
    boolean isOngoingCmSubscription(final DatastoreType datastoreType, final String cmHandleId, final String xpath);
}
