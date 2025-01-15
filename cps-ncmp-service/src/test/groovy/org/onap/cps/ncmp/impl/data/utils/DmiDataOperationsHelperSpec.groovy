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

package org.onap.cps.ncmp.impl.data.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.impl.KafkaHeaders
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.data.models.DataOperationRequest
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.config.CpsApplicationContext
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.impl.data.models.DmiDataOperation
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.LinkedMultiValueMap

import java.time.Duration

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY
import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent

@ContextConfiguration(classes = [EventsPublisher, CpsApplicationContext])
class DmiDataOperationsHelperSpec extends MessagingBaseSpec {

    def static clientTopic = 'my-topic-name'
    def static dataOperationType = 'org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent'

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @SpringBean
    EventsPublisher eventPublisher = new EventsPublisher<CloudEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    def 'Process per data operation request with #serviceName.'() {
        given: 'data operation request with 3 operations'
            def dataOperationRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = jsonObjectMapper.convertJsonString(dataOperationRequestJsonData, DataOperationRequest.class)
        and: '4 known cm handles: ch1-dmi1, ch2-dmi1, ch3-dmi2, ch4-dmi2'
            def yangModelCmHandles = getYangModelCmHandles()
        when: 'data operation request is processed'
            def operationsOutPerDmiServiceName = DmiDataOperationsHelper.processPerDefinitionInDataOperationsRequest(clientTopic,'request-id', dataOperationRequest, yangModelCmHandles)
        and: 'converted to a json node'
            def dmiDataOperationRequestBody = jsonObjectMapper.asJsonString(operationsOutPerDmiServiceName.get(serviceName))
            def dmiDataOperationRequestBodyAsJsonNode = jsonObjectMapper.convertToJsonNode(dmiDataOperationRequestBody).get(operationIndex)
        then: 'it contains the correct operation details'
            assert dmiDataOperationRequestBodyAsJsonNode.get('operation').asText() == 'read'
            assert dmiDataOperationRequestBodyAsJsonNode.get('operationId').asText() == expectedOperationId
            assert dmiDataOperationRequestBodyAsJsonNode.get('datastore').asText() == expectedDatastore
        and: 'the correct cm handles (just for #serviceName)'
            assert dmiDataOperationRequestBodyAsJsonNode.get('cmHandles').size() == expectedCmHandleReferences.size()
            expectedCmHandleReferences.each {
                dmiDataOperationRequestBodyAsJsonNode.get('cmHandles').toString().contains(it)
            }
        where: 'the following dmi service and operations are checked'
            serviceName | operationIndex || expectedOperationId | expectedDatastore                        | expectedCmHandleReferences
            'dmi1'      | 0              || 'operational-14'    | 'ncmp-datastore:passthrough-operational' | ['ch6-dmi1']
            'dmi1'      | 1              || 'running-12'        | 'ncmp-datastore:passthrough-running'     | ['ch1-dmi1', 'ch2-dmi1']
            'dmi1'      | 2              || 'operational-15'    | 'ncmp-datastore:passthrough-operational' | ['ch6-dmi1']
            'dmi1'      | 3              || 'operational-16'    | 'ncmp-datastore:passthrough-operational' | ['alt6-dmi1']
            'dmi2'      | 0              || 'operational-14'    | 'ncmp-datastore:passthrough-operational' | ['ch3-dmi2']
            'dmi2'      | 1              || 'running-12'        | 'ncmp-datastore:passthrough-running'     | ['ch7-dmi2']
            'dmi2'      | 2              || 'operational-15'    | 'ncmp-datastore:passthrough-operational' | ['ch4-dmi2']
    }

    def 'Process one data operation request with #serviceName and Module Set Tag set.'() {
        given: 'data operation request'
            def dataOperationRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = jsonObjectMapper.convertJsonString(dataOperationRequestJsonData, DataOperationRequest.class)
        and: '1 known cm handles: ch1-dmi1'
            def yangModelCmHandles = getYangModelCmHandlesForOneCmHandle()
        when: 'data operation request is processed'
            def operationsOutPerDmiServiceName = DmiDataOperationsHelper.processPerDefinitionInDataOperationsRequest(clientTopic,'request-id', dataOperationRequest, yangModelCmHandles)
        and: 'converted to a json node'
            def dmiDataOperationRequestBody = operationsOutPerDmiServiceName['dmi1']
            def cmHandlesInRequestBody = dmiDataOperationRequestBody[0].cmHandles
        then: 'it contains the correct operation details'
            assert cmHandlesInRequestBody.size() == 1
            assert cmHandlesInRequestBody[0].id == 'ch1-dmi1'
            assert cmHandlesInRequestBody[0].moduleSetTag == 'module-set-tag1'
    }

    def 'Process per data operation request with non-ready, non-existing cm handle and publish event to client specified topic'() {
        given: 'consumer subscribing to client topic'
            def cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test-1', CloudEventDeserializer))
            cloudEventKafkaConsumer.subscribe([clientTopic])
        and: 'data operation request having non-ready and non-existing cm handle ids'
            def dataOperationRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = jsonObjectMapper.convertJsonString(dataOperationRequestJsonData, DataOperationRequest.class)
        when: 'data operation request is processed'
            DmiDataOperationsHelper.processPerDefinitionInDataOperationsRequest(clientTopic, 'request-id', dataOperationRequest, yangModelCmHandles)
        and: 'subscribed client specified topic is polled and first record is selected'
            def consumerRecordOut = cloudEventKafkaConsumer.poll(Duration.ofMillis(1500)).last()
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
        def readyState = new CompositeStateBuilder().withCmHandleState(READY).withLastUpdatedTimeNow().build()
        def advisedState = new CompositeStateBuilder().withCmHandleState(ADVISED).withLastUpdatedTimeNow().build()
        return [new YangModelCmHandle(id: 'ch1-dmi1', 'alternateId': 'alt1-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch2-dmi1', 'alternateId': 'alt2-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch6-dmi1', 'alternateId': 'alt6-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch8-dmi1', 'alternateId': 'alt8-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch3-dmi2', 'alternateId': 'alt3-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch4-dmi2', 'alternateId': 'alt4-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch7-dmi2', 'alternateId': 'alt7-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'non-ready-cm-handle', 'alternateId': 'non-ready-alternate', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: advisedState)
        ]
    }

    static def getYangModelCmHandlesForOneCmHandle() {
        def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]
        def readyState = new CompositeStateBuilder().withCmHandleState(READY).withLastUpdatedTimeNow().build()
        return [new YangModelCmHandle(id: 'ch1-dmi1', dmiServiceName: 'dmi1', moduleSetTag: 'module-set-tag1', dmiProperties: dmiProperties, compositeState: readyState)]
    }

    def mockAndPopulateErrorMap(errorReportedToClientTopic) {
        def dmiDataOperation = DmiDataOperation.builder().operation(OperationType.fromOperationName('read'))
                .operationId('some-op-id').datastore('ncmp-datastore:passthrough-operational')
                .options('some-option').resourceIdentifier('some-resource-identifier').build()
        def cmHandleIdsPerResponseCodesPerOperation = new LinkedMultiValueMap<>()
        cmHandleIdsPerResponseCodesPerOperation.add(dmiDataOperation, Map.of(errorReportedToClientTopic, ['some-cm-handle-id']))
        return cmHandleIdsPerResponseCodesPerOperation
    }
}
