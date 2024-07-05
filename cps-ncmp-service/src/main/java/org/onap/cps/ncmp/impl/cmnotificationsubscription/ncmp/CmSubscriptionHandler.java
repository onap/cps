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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp;

import java.util.List;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.Predicate;

public interface CmSubscriptionHandler {

    /**
     * Process cm notification subscription create request.
     *
     * @param subscriptionId subscription id
     * @param predicates subscription predicates
     */
    void processSubscriptionCreateRequest(final String subscriptionId, final List<Predicate> predicates);

    /**
     * Process cm notification subscription delete request.
     *
     * @param subscriptionId subscription id
     * @param predicates subscription predicates
     */
    void processSubscriptionDeleteRequest(final String subscriptionId, final List<Predicate> predicates);

}