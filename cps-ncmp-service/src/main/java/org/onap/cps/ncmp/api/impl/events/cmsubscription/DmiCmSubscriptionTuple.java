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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import java.util.Collection;
import java.util.Map;

/**
 * Tuple to be used during the delete usecase.
 *
 * @param lastSubscriberPerDmi   these are the subscribers that are unique for the dmi
 * @param otherSubscribersPerDmi these are the shared subscribers which share the details with others
 */
public record DmiCmSubscriptionTuple(Map<String, Collection<DmiCmSubscriptionKey>> lastSubscriberPerDmi,
                                     Map<String, Collection<DmiCmSubscriptionKey>> otherSubscribersPerDmi) {


}
