/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.impl.cache

import com.hazelcast.topic.ITopic
import com.hazelcast.topic.Message
import org.onap.cps.impl.YangTextSchemaSourceSetCache
import spock.lang.Specification

class YangSchemaCacheEvictionListenerSpec extends Specification {

    def mockYangSchemaCacheEvictionTopic = Mock(ITopic)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def objectUnderTest = new YangSchemaCacheEvictionListener(mockYangSchemaCacheEvictionTopic, mockYangTextSchemaSourceSetCache)

    def capturedMessageListener

    def setup() {
        mockYangSchemaCacheEvictionTopic.addMessageListener(_) >> { args -> capturedMessageListener = args[0]; return }
        objectUnderTest.subscribeToEvictionTopic()
    }

    def 'Eviction message with valid cache key.'() {
        when: 'a valid eviction message is received'
            capturedMessageListener.onMessage(mockMessage('myDataspace-mySchemaSet'))
        then: 'the local cache is evicted for the correct dataspace and schema set'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('myDataspace', 'mySchemaSet')
    }

    def 'Eviction message with malformed key.'() {
        when: 'a malformed eviction message is received (no dash separator)'
            capturedMessageListener.onMessage(mockMessage('invalid key (no dash)'))
        then: 'no cache eviction is attempted'
            0 * mockYangTextSchemaSourceSetCache.removeFromCache(*_)
    }

    def mockMessage(messageObject) {
        def mockedMessage = Mock(Message)
        mockedMessage.getMessageObject() >> messageObject
        return mockedMessage
    }
}
