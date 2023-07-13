/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.subscriptions

import org.onap.cps.spi.exceptions.SubscriptionOutcomeTypeNotFoundException
import spock.lang.Specification

class SubscriptionOutcomeTypeSpec extends Specification  {


    def 'Get a subscription outcome type from a code that exists successfully'() {
        when: 'the subscription response event is mapped to a subscription event outcome'
            def result = SubscriptionOutcomeType.fromCode(600)
        then: 'the resulting subscription outcome type as expected'
            result == SubscriptionOutcomeType.SUCCESS
            result.outcomeCode() == 600
    }

    def 'Get a subscription outcome type from a code that does not exists'() {
        when: 'the subscription response event is mapped to a subscription event outcome'
            def thrownException = null
            try {
                def result = SubscriptionOutcomeType.fromCode(700)
            } catch (Exception ex) {
                thrownException = ex
            }
        then: 'a subscription outcome type not found exception thrown'
            thrownException instanceof SubscriptionOutcomeTypeNotFoundException
            thrownException.details.contains('Subscription outcome type not found')
    }

}
