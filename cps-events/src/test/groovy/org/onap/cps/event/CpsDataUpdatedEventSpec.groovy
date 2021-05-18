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

    static final String DATASPACE_NAME = 'my-dataspace'
    static final String BOOKSTORE_SCHEMA_SET = 'bootstore-schemaset'
    static final String ANCHOR_NAME = 'chapters'
    static final String EVENT_TIMESTAMP = '2020-12-01T00:00:00.000+0000'
    static final String EVENT_ID = '77b8f114-4562-4069-8234-6d059ff742ac'
    static final URI EVENT_SOURCE = new URI('urn:cps:org.onap.cps')
    static final String EVENT_TYPE = 'org.onap.cps.data-updated-event'
    static final String EVENT_SCHEMA = 'urn:cps:org.onap.cps:data-updated-event-schema:1.1.0-SNAPSHOT'

    def DATA = [
            'test:bookstore': [
                    'bookstore-name':'Chapters',
                    'categories': [
                            ['code':'01',
                             'name':'SciFi',
                             'books':[
                                     [ 'authors' :[ 'Iain M. Banks'],
                                       'lang':'en',
                                       'price':'895',
                                       'pub_year':'1994',
                                       'title':'Feersum Endjinn'
                                     ]
                             ]
                            ],
                            [ 'name': 'kids',
                              'code':'02',
                              'books':[
                                      [ 'authors':['Philip Pullman'],
                                        'lang':'en',
                                        'price':'699',
                                        'pub_year':'1995',
                                        'title':'The Golden Compass'
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
            content.timestamp == EVENT_TIMESTAMP
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
                    .withTimestamp(EVENT_TIMESTAMP)
                    .withData(data)
            cpsDataUpdateEvent
                    .withSchema(
                            CpsDataUpdatedEvent.Schema.fromValue(EVENT_SCHEMA))
                    .withId(EVENT_ID)
                    .withSource(EVENT_SOURCE)
                    .withType(EVENT_TYPE)
                    .withContent(content)
        expect : 'comparing JSON created from CpsDataUpdatedEvent with the Notification Message'
             objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cpsDataUpdateEvent) == notificationMessage
    }

}