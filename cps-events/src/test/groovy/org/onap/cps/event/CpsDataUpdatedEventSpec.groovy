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
import spock.lang.Specification

/**
 * Test class for CpsDataUpdatedEvent.
 */
class CpsDataUpdatedEventSpec extends Specification {

    private String notificationMessage;
    def objectMapper = new ObjectMapper()

    final DATASPACE_NAME = 'my-dataspace'
    final BOOKSTORE_SCHEMA_SET = 'bootstore-schemaset'
    final ANCHOR_NAME = 'chapters'
    final EVENT_TIMESTAMP = '2020-12-01T00:00:00.000+0000'
    final EVENT_ID = '77b8f114-4562-4069-8234-6d059ff742ac'
    final EVENT_SOURCE = new URI('urn:cps:org.onap.cps')
    final EVENT_TYPE = 'org.onap.cps.data-updated-event'
    final EVENT_SCHEMA = 'urn:cps:org.onap.cps:data-updated-event-schema:1.1.0-SNAPSHOT'

    final DATA = [
            'test:bookstore': [
                    'bookstore-name': 'Chapters',
                    'categories': [
                            ['code': '01',
                             'name': 'SciFi',
                             'books':[
                                     [ 'authors' : [ 'Iain M. Banks'],
                                       'lang': 'en',
                                       'price': 895,
                                       'pub_year': '1994',
                                       'title': 'Feersum Endjinn'
                                     ]
                             ]
                            ]
                    ]
            ]
    ]

    def setup() {
        notificationMessage = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                objectMapper.readValue(
                        this.class.getResource('/bookstore-chapters.json').getText('UTF-8'),
                        ObjectNode.class)
        )
    }

    def 'Conversion from JSON String to Java POJO.'() {
        when: 'converting to CpsDataUpdatedEvent'
            def cpsDataUpdatedEvent = objectMapper.readValue(notificationMessage, CpsDataUpdatedEvent.class)
        then: 'comparing pojo with expected values'
            cpsDataUpdatedEvent.id == EVENT_ID
            cpsDataUpdatedEvent.source == EVENT_SOURCE
            cpsDataUpdatedEvent.schema.value() == EVENT_SCHEMA
            cpsDataUpdatedEvent.type == EVENT_TYPE
            def content = cpsDataUpdatedEvent.content
            content.observedTimestamp == EVENT_TIMESTAMP
            content.dataspaceName == DATASPACE_NAME
            content.schemaSetName == BOOKSTORE_SCHEMA_SET
            content.anchorName == ANCHOR_NAME
            content.data.getAdditionalProperties() == DATA
    }

    def 'Conversion from Java POJO to JSON String.'() {
        given: 'creating the CpsDataUpdatedEvent'
            def cpsDataUpdateEvent = new CpsDataUpdatedEvent();
            def data = new Data();
            data.withAdditionalProperty('test:bookstore', DATA.'test:bookstore')
            def content = new Content()
            content.withAnchorName(ANCHOR_NAME)
                    .withDataspaceName(DATASPACE_NAME)
                    .withSchemaSetName(BOOKSTORE_SCHEMA_SET)
                    .withObservedTimestamp(EVENT_TIMESTAMP)
                    .withData(data)
            cpsDataUpdateEvent
                    .withSchema(
                            CpsDataUpdatedEvent.Schema.fromValue(EVENT_SCHEMA))
                    .withId(EVENT_ID)
                    .withSource(EVENT_SOURCE)
                    .withType(EVENT_TYPE)
                    .withContent(content)
        when: 'converting to JSON string'
            def actualMessage = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cpsDataUpdateEvent)
        then : 'comparing JSON String with the expected Notification Message'
             actualMessage == notificationMessage
    }

}