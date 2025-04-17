/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration.functional.ncmp

import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import java.time.Duration
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.integration.KafkaTestContainer
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.events.async1_0_0.Data
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.events.async1_0_0.Response
import org.springframework.http.MediaType
import spock.util.concurrent.PollingConditions

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class LegacyBatchDataOperationSpec extends CpsIntegrationSpecBase {

    KafkaConsumer kafkaConsumer

    def setup() {
        kafkaConsumer = KafkaTestContainer.getConsumer('test-group', CloudEventDeserializer.class)
        kafkaConsumer.subscribe(['legacy-batch-topic'])
        kafkaConsumer.poll(Duration.ofMillis(500))
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-2'] = ['M1', 'M2']
        registerCmHandle(DMI1_URL, 'ch-1', 'tagA', 'alt-1')
        registerCmHandle(DMI1_URL, 'ch-2', 'tagB', 'alt-2')
        registerCmHandleWithoutWaitForReady(DMI1_URL, 'not-ready-ch', NO_MODULE_SET_TAG, 'alt-3')
    }

    def cleanup() {
        deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2', 'not-ready-ch'])
        kafkaConsumer.unsubscribe()
        kafkaConsumer.close()
    }

    def 'Batch pass-through data operation is forwarded to DMI plugin.'() {
        when: 'a pass-through batch data request is sent to NCMP is successful'
            mvc.perform(post('/ncmp/v1/data')
                    .queryParam('topic', 'legacy-batch-topic')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                               {
                                    "operations": [
                                        {
                                            "operation": "read",
                                            "operationId": "12",
                                            "datastore": "ncmp-datastore:passthrough-operational",
                                            "resourceIdentifier": "ManagedElement=NRNode1/GNBDUFunction=1",
                                            "options": "(fields=NRCellDU/attributes/cellLocalId)",
                                            "targetIds": ["ch-1", "ch-2"]
                                        }
                                    ]
                                }
                        """)
            ).andExpect(status().is2xxSuccessful())

        then: 'DMI will receive the async request with the expected body'
            new PollingConditions().within(2, () -> {
                assert dmiDispatcher1.dmiDataOperationRequestBody == '{"operations":[{"operation":"read","operationId":"12","datastore":"ncmp-datastore:passthrough-operational","options":"(fields=NRCellDU/attributes/cellLocalId)","resourceIdentifier":"ManagedElement=NRNode1/GNBDUFunction=1","cmHandles":[{"id":"ch-1","moduleSetTag":"tagA","cmHandleProperties":{}},{"id":"ch-2","moduleSetTag":"tagB","cmHandleProperties":{}}]}]}'
            })
    }

    def 'Batch pass-through data operation reports errors on kafka topic.'() {
        when: 'a pass-through batch data request is sent to NCMP specifying a kafka topic is successful'
            mvc.perform(post('/ncmp/v1/data')
                    .queryParam('topic', 'legacy-batch-topic')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                           {
                                "operations": [
                                    {
                                        "resourceIdentifier": "ManagedElement=NRNode1/GNBDUFunction=1",
                                        "targetIds": ["%s"],
                                        "datastore": "ncmp-datastore:passthrough-operational",
                                        "options": "(fields=NRCellDU/attributes/cellLocalId)",
                                        "operationId": "12",
                                        "operation": "read"
                                    }
                                ]
                            }
                    """.formatted(cmHandleId))
            ).andExpect(status().is2xxSuccessful())

        then: 'there is one kafka message'
            def consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000))
            assert consumerRecords.size() == 1

        and: 'it is a cloud event'
            assert consumerRecords[0].value() instanceof CloudEvent

        and: 'it contains the data operation event with the expected error status'
            def jsonData = new String(consumerRecords[0].value().data.toBytes())
            def dataOperationEvent = jsonObjectMapper.convertJsonString(jsonData, DataOperationEvent)
            assert dataOperationEvent == new DataOperationEvent(data:
                    new Data(responses: [
                            new Response(
                                operationId: 12,
                                resourceIdentifier: 'ManagedElement=NRNode1/GNBDUFunction=1',
                                options: '(fields=NRCellDU/attributes/cellLocalId)',
                                ids: [cmHandleId],
                                statusCode: expectedStatusCode,
                                statusMessage: expectedStatusMessage,
                                result: null),
                    ]))

        where:
            scenario              | cmHandleId     || expectedStatusCode | expectedStatusMessage
            'CM handle not ready' | 'not-ready-ch' || 101                | 'cm handle(s) not ready'
            // FIXME BUG CPS-2769: CM handle not found causes batch to fail
            // 'CM handle not found' | 'not-found-ch' || 100                | 'cm handle reference(s) not found'
    }

}
