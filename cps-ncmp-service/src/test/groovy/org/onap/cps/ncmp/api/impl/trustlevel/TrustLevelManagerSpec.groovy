/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel

import org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient.AvcEventPublisher
import spock.lang.Specification

class TrustLevelManagerSpec extends Specification {

    def trustLevelPerCmHandle = [:]
    def mockAttributeValueChangeEventPublisher = Mock(AvcEventPublisher)
    def objectUnderTest = new TrustLevelManager(trustLevelPerCmHandle, mockAttributeValueChangeEventPublisher)

    def 'Initial cm handle registration'() {
        given: 'two cm handles: one with no trust level and one trusted'
            def cmHandleModelsToBeCreated = ['ch-1': null, 'ch-2': TrustLevel.COMPLETE]
        when: 'the initial registration handled'
            objectUnderTest.handleInitialRegistrationOfTrustLevels(cmHandleModelsToBeCreated)
        then: 'no notification sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
        and: 'both cm handles are in the cache and are trusted'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.COMPLETE
            assert trustLevelPerCmHandle.get('ch-2') == TrustLevel.COMPLETE
    }

    def 'Initial cm handle registration with a cm handle that is not trusted'() {
        given: 'a not trusted cm handle'
            def cmHandleModelsToBeCreated = ['ch-2': TrustLevel.NONE]
        when: 'the initial registration handled'
            objectUnderTest.handleInitialRegistrationOfTrustLevels(cmHandleModelsToBeCreated)
        then: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

    def 'Trust level updated'() {
        given: 'a not trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        when: 'the update is handled'
            objectUnderTest.handleUpdateOfTrustLevels('ch-1', 'COMPLETE')
        then: 'notification is sent'
            1 * mockAttributeValueChangeEventPublisher.publishAvcEvent('ch-1', 'trustLevel', 'NONE', 'COMPLETE')
        and: 'the cm handle in the cache is trusted'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.COMPLETE
    }

    def 'Trust level updated with same value'() {
        given: 'a trusted cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        when: 'the update is handled'
            objectUnderTest.handleUpdateOfTrustLevels('ch-1', 'COMPLETE')
        then: 'no notification is sent'
            0 * mockAttributeValueChangeEventPublisher.publishAvcEvent(*_)
    }

}
