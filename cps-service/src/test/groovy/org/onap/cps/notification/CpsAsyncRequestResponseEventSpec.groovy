/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022 Nordix Foundation.
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

package org.onap.cps.notification

import org.onap.cps.event.model.CpsAsyncRequestResponseEvent
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Ignore
import spock.lang.Specification

@SpringBootTest(classes = [cpsasyncrequestresponseeventConsumer,
    CpsAsyncRequestResponseEventProducer,
    CpsAsyncRequestResponseEventPublisherService])
class CpsAsyncRequestResponseEventSpec extends Specification {

    @SpringBean
    KafkaTemplate kafakTemplate = Mock()

    @SpringBean
    CpsAsyncRequestResponseEventProducer next1 = new CpsAsyncRequestResponseEventProducer(kafakTemplate)

    @SpringBean
    CpsAsyncRequestResponseEventPublisherService next = new CpsAsyncRequestResponseEventPublisherService(next1)

    @SpringBean
    cpsasyncrequestresponseeventConsumer objectUnderTest = new cpsasyncrequestresponseeventConsumer(CpsAsyncRequestResponseEventPublisherService)

    // TODO: @Joe - fix
    @Ignore
    def 'Create a CPS data updated event successfully'() {
        given: 'a CpsAsyncRequestResponseEvent'
            def cpsAsync = new CpsAsyncRequestResponseEvent()
        when: 'event is consumed'
            objectUnderTest.consume(cpsAsync)
        then: 'the event is forwarded to client topic'
            next.publishEvent(_, cpsAsync)
    }
}
