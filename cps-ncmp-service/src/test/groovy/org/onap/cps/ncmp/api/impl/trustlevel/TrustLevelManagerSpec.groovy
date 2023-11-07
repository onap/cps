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

import org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient.AttributeValueChangeEventPublisher
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import spock.lang.Specification

class TrustLevelManagerSpec extends Specification {

    def trustLevelPerCmHandle = [:]
    def mockAttributeValueChangeEventPublisher = Mock(AttributeValueChangeEventPublisher)
    def objectUnderTest = new TrustLevelManager(trustLevelPerCmHandle, mockAttributeValueChangeEventPublisher)

    def 'Some notification sent during initial cm registration'() {
        given: 'a list of cm handle to be created that dont exists in the cache'
            def cmHandleModel1 = new NcmpServiceCmHandle(cmHandleId: 'ch-1', registrationTrustLevel: TrustLevel.COMPLETE)
            def cmHandleModel2 = new NcmpServiceCmHandle(cmHandleId: 'ch-2', registrationTrustLevel: TrustLevel.NONE)
        when: 'notification method run'
            objectUnderTest.handleInitialRegistrationOfTrustLevels([cmHandleModel1, cmHandleModel2])
        then: 'a notification for "TrustLevel.NONE" has been sent'
            1 * mockAttributeValueChangeEventPublisher.publishAttributeValueChangeEvent('ch-2', 'trustLevel', null, TrustLevel.NONE.name())
        and: 'both cm handles populated in the cache'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.COMPLETE
            assert trustLevelPerCmHandle.get('ch-2') == TrustLevel.NONE
    }

    def 'No notification sent during initial cm registration'() {
        given: 'the cache has been initialised and "knows" about my cm handles'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
            trustLevelPerCmHandle.put('ch-2', TrustLevel.NONE)
        and: 'a list of cm handle to be created that are already exists'
            def cmHandleModel1 = new NcmpServiceCmHandle(cmHandleId: 'ch-1', registrationTrustLevel: TrustLevel.COMPLETE)
            def cmHandleModel2 = new NcmpServiceCmHandle(cmHandleId: 'ch-2', registrationTrustLevel: TrustLevel.NONE)
        when: 'notification method run'
            objectUnderTest.handleInitialRegistrationOfTrustLevels([cmHandleModel1, cmHandleModel2])
        then: 'no notification has been sent'
            0 * mockAttributeValueChangeEventPublisher.publishAttributeValueChangeEvent()
    }

    def 'Some notification sent during trust level update'() {
        given: 'the cache has been initialised and "knows" about my cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        when: 'notification method run with updated trust level'
            objectUnderTest.handleUpdateOfTrustLevels('ch-1', TrustLevel.COMPLETE.name())
        then: 'a notification for the updated trust level sent'
            1 * mockAttributeValueChangeEventPublisher.publishAttributeValueChangeEvent('ch-1', 'trustLevel', TrustLevel.NONE.name(), TrustLevel.COMPLETE.name())
        and: 'the cache has been populated'
            assert trustLevelPerCmHandle.get('ch-1') == TrustLevel.COMPLETE
    }

    def 'No notification sent during trust level update'() {
        given: 'the cache has been initialised and "knows" about my cm handle'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.NONE)
        when: 'notification method run with the same trust level'
            objectUnderTest.handleUpdateOfTrustLevels('ch-1', TrustLevel.NONE.name())
        then: 'no notification sent at all'
            0 * mockAttributeValueChangeEventPublisher.publishAttributeValueChangeEvent('ch-1', 'trustLevel', TrustLevel.NONE.name(), TrustLevel.NONE.name())
    }

}
