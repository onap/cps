/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils.data.operation

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.impl.KafkaHeaders
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.utils.context.CpsApplicationContext
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import java.time.Duration

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

@ContextConfiguration(classes = [EventsPublisher, CpsApplicationContext, ObjectMapper])
class ResourceDataOperationRequestUtilsSpec extends MessagingBaseSpec {

    def cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test', CloudEventDeserializer))
    def static clientTopic = 'my-topic-name'
    def static dataOperationType = 'org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent'

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @SpringBean
    EventsPublisher eventPublisher = new EventsPublisher<CloudEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    @Autowired
    ObjectMapper objectMapper

    def 'Process per data operation request with #serviceName.'() {
        given: 'data operation request with 3 operations'
            def dataOperationRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = jsonObjectMapper.convertJsonString(dataOperationRequestJsonData, DataOperationRequest.class)
        and: '4 known cm handles: ch1-dmi1, ch2-dmi1, ch3-dmi2, ch4-dmi2'
            def yangModelCmHandles = getYangModelCmHandles()
        when: 'data operation request is processed'
            def operationsOutPerDmiServiceName = ResourceDataOperationRequestUtils.processPerDefinitionInDataOperationsRequest(clientTopic,'request-id', dataOperationRequest, yangModelCmHandles)
        and: 'converted to a json node'
            def dmiDataOperationRequestBody = jsonObjectMapper.asJsonString(operationsOutPerDmiServiceName.get(serviceName))
            def dmiDataOperationRequestBodyAsJsonNode = jsonObjectMapper.convertToJsonNode(dmiDataOperationRequestBody).get(operationIndex)
        then: 'it contains the correct operation details'
            assert dmiDataOperationRequestBodyAsJsonNode.get('operation').asText() == 'read'
            assert dmiDataOperationRequestBodyAsJsonNode.get('operationId').asText() == expectedOperationId
            assert dmiDataOperationRequestBodyAsJsonNode.get('datastore').asText() == expectedDatastore
        and: 'the correct cm handles (just for #serviceName)'
            assert dmiDataOperationRequestBodyAsJsonNode.get('cmHandles').size() == expectedCmHandleIds.size()
            expectedCmHandleIds.each {
                dmiDataOperationRequestBodyAsJsonNode.get('cmHandles').toString().contains(it)
            }
        where: 'the following dmi service and operations are checked'
            serviceName | operationIndex || expectedOperationId | expectedDatastore                        | expectedCmHandleIds
            'dmi1'      | 0              || 'operational-14'    | 'ncmp-datastore:passthrough-operational' | ['ch6-dmi1']
            'dmi1'      | 1              || 'running-12'        | 'ncmp-datastore:passthrough-running'     | ['ch1-dmi1', 'ch2-dmi1']
            'dmi1'      | 2              || 'operational-15'    | 'ncmp-datastore:passthrough-operational' | ['ch6-dmi1']
            'dmi2'      | 0              || 'operational-14'    | 'ncmp-datastore:passthrough-operational' | ['ch3-dmi2']
            'dmi2'      | 1              || 'running-12'        | 'ncmp-datastore:passthrough-running'     | ['ch7-dmi2']
            'dmi2'      | 2              || 'operational-15'    | 'ncmp-datastore:passthrough-operational' | ['ch4-dmi2']
    }

    def 'Process per data operation request with non-ready, non-existing cm handle and publish event to client specified topic'() {
        given: 'consumer subscribing to client topic'
            cloudEventKafkaConsumer.subscribe([clientTopic])
        and: 'data operation request having non-ready and non-existing cm handle ids'
            def dataOperationRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = jsonObjectMapper.convertJsonString(dataOperationRequestJsonData, DataOperationRequest.class)
        when: 'data operation request is processed'
            ResourceDataOperationRequestUtils.processPerDefinitionInDataOperationsRequest(clientTopic, 'request-id', dataOperationRequest, yangModelCmHandles)
        and: 'subscribed client specified topic is polled and first record is selected'
            def consumerRecordOut = cloudEventKafkaConsumer.poll(Duration.ofMillis(1500))[0]
        then: 'verify cloud compliant headers'
            def consumerRecordOutHeaders = consumerRecordOut.headers()
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_id') != null
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_type') == dataOperationType
        and: 'verify that extension is included into header'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_correlationid') == 'request-id'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_destination') == clientTopic
        and: 'map consumer record to expected event type'
            def dataOperationResponseEvent =
                toTargetEvent(consumerRecordOut.value(), DataOperationEvent.class)
        and: 'data operation response event response size is 3'
            dataOperationResponseEvent.data.responses.size() == 3
        and: 'verify published data operation response as json string'
        def dataOperationResponseEventJson = TestUtils.getResourceFileContent('dataOperationResponseEvent.json')
            jsonObjectMapper.asJsonString(dataOperationResponseEvent.data.responses) == dataOperationResponseEventJson
    }

    static def getYangModelCmHandles() {
        def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]
        def readyState = new CompositeStateBuilder().withCmHandleState(CmHandleState.READY).withLastUpdatedTimeNow().build()
        def advisedState = new CompositeStateBuilder().withCmHandleState(CmHandleState.ADVISED).withLastUpdatedTimeNow().build()
        return [new YangModelCmHandle(id: 'ch1-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch2-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch6-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch8-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch3-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch4-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch7-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'non-ready-cm-handle', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: advisedState)
        ]
    }
}
