/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
*/

package org.onap.cps.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.onap.cps.event.model.Content
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.onap.cps.event.model.Data
import spock.lang.Shared
import spock.lang.Specification

/**
 * Test class for CpsDataUpdatedEvent.
 */
class CpsDataUpdatedEventSpec extends Specification {

    def objectMapper = new ObjectMapper()

    final DATASPACE_NAME = 'my-dataspace'
    final BOOKSTORE_SCHEMA_SET = 'bookstore-schemaset'
    final ANCHOR_NAME = 'chapters'
    final EVENT_TIMESTAMP = '2020-12-01T00:00:00.000+0000'
    final EVENT_ID = '77b8f114-4562-4069-8234-6d059ff742ac'
    final EVENT_SOURCE = new URI('urn:cps:org.onap.cps')
    final EVENT_TYPE = 'org.onap.cps.data-updated-event'
    final EVENT_SCHEMA_V1 = new URI('urn:cps:org.onap.cps:data-updated-event-schema:v1')
    final EVENT_SCHEMA_V2 = new URI('urn:cps:org.onap.cps:data-updated-event-schema:v2')

    @Shared
    final DATA = [
            'test:bookstore': [
                    'bookstore-name': 'Chapters',
                    'categories'    : [
                            ['code' : '01',
                             'name' : 'SciFi',
                             'books': [
                                     ['authors' : ['Iain M. Banks'],
                                      'lang'    : 'en',
                                      'price'   : 895,
                                      'pub_year': '1994',
                                      'title'   : 'Feersum Endjinn'
                                     ]
                             ]
                            ]
                    ]
            ]
    ]

    def 'Conversion from Event V1 JSON String to CpsDataUpdatedEvent POJO.'() {
        when: 'event V1 JSON String is converted to CpsDataUpdatedEvent'
            def notificationMessage = getEventAsJsonStringFromFile('/event-v1.json')
            def cpsDataUpdatedEvent = objectMapper.readValue(notificationMessage, CpsDataUpdatedEvent.class)
        then: 'CpsDataUpdatedEvent POJO has the excepted values'
            cpsDataUpdatedEvent.id == EVENT_ID
            cpsDataUpdatedEvent.source == EVENT_SOURCE
            cpsDataUpdatedEvent.schema == EVENT_SCHEMA_V1
            cpsDataUpdatedEvent.type == EVENT_TYPE
            def content = cpsDataUpdatedEvent.content
            content.observedTimestamp == EVENT_TIMESTAMP
            content.dataspaceName == DATASPACE_NAME
            content.schemaSetName == BOOKSTORE_SCHEMA_SET
            content.anchorName == ANCHOR_NAME
            content.data.getAdditionalProperties() == DATA
    }

    def 'Conversion from Event V2 JSON String to CpsDataUpdatedEvent POJO'() {
        when: 'event V2 JSON String is converted to CpsDataUpdatedEvent'
            def notificationMessage = getEventAsJsonStringFromFile(inputEventJson)
            def cpsDataUpdatedEvent = objectMapper.readValue(notificationMessage, CpsDataUpdatedEvent.class)
        then: 'CpsDataUpdatedEvent POJO has the excepted values'
            with(cpsDataUpdatedEvent) {
                id == EVENT_ID
                source == EVENT_SOURCE
                schema == EVENT_SCHEMA_V2
                type == EVENT_TYPE
            }
            with(cpsDataUpdatedEvent.content) {
                observedTimestamp == EVENT_TIMESTAMP
                dataspaceName == DATASPACE_NAME
                schemaSetName == BOOKSTORE_SCHEMA_SET
                anchorName == ANCHOR_NAME
                operation == expectedOperation
                if (expectedData != null)
                    data.getAdditionalProperties() == expectedData
                else
                    data == null
            }
        where:
            scenario                        | inputEventJson                              || expectedData | expectedOperation
            'create operation'              | '/event-v2-create-operation.json'           || DATA         | Content.Operation.CREATE
            'delete operation'              | '/event-v2-delete-operation.json'           || null         | Content.Operation.DELETE
            'create with additional fields' | '/event-v2-with-additional-properties.json' || DATA         | Content.Operation.CREATE
    }

    def 'Conversion from CpsDataUpdatedEvent POJO to Event V2 JSON String.'() {
        given: 'Event V2 content with the Data'
            def data = new Data()
            data.withAdditionalProperty('test:bookstore', DATA.'test:bookstore')
            def content = new Content()
            content.withAnchorName(ANCHOR_NAME)
                    .withDataspaceName(DATASPACE_NAME)
                    .withSchemaSetName(BOOKSTORE_SCHEMA_SET)
                    .withObservedTimestamp(EVENT_TIMESTAMP)
                    .withOperation(Content.Operation.CREATE)
                    .withData(data)
        and: 'CpsDataUpdatedEvent with the content'
            def cpsDataUpdateEvent = new CpsDataUpdatedEvent()
            cpsDataUpdateEvent
                    .withSchema(EVENT_SCHEMA_V2)
                    .withId(EVENT_ID)
                    .withSource(EVENT_SOURCE)
                    .withType(EVENT_TYPE)
                    .withContent(content)
        when: 'CpsDataUpdatedEvent is converted to Event V2 JSON string'
            def actualMessage = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cpsDataUpdateEvent)
        then: 'the created JSON String is same as the expected JSON String'
            def expectedMessage = getEventAsJsonStringFromFile('/event-v2-create-operation.json')
            assert actualMessage == expectedMessage
    }

    def getEventAsJsonStringFromFile(String fileName) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                objectMapper.readValue(
                        this.class.getResource(fileName).getText('UTF-8'),
                        ObjectNode.class)
        )
    }
}
