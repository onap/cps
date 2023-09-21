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

package org.onap.cps.ncmp.api.impl.subscriptions

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NO_TIMESTAMP

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class SubscriptionPersistenceSpec extends Specification {

    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_PARENT = "/subscription-registry";
    private static final String SUBSCRIPTION_REGISTRY_PREDICATES_XPATH = "/subscription-registry/subscription[@clientID='some-client-id' and @subscriptionName='some-subscription-name']/predicates";

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCpsDataService = Mock(CpsDataService)
    def objectUnderTest = new SubscriptionPersistenceImpl(jsonObjectMapper, mockCpsDataService)

    def predicates = new YangModelSubscriptionEvent.Predicates(datastore: 'some-datastore',
        targetCmHandles: [new YangModelSubscriptionEvent.TargetCmHandle('cmhandle1'),
                          new YangModelSubscriptionEvent.TargetCmHandle('cmhandle2')])
    def yangModelSubscriptionEvent = new YangModelSubscriptionEvent(clientId: 'some-client-id',
        subscriptionName: 'some-subscription-name', tagged: true, topic: 'some-topic', predicates: predicates)

   def 'save a subscription event as yang model into db for the #scenarios' () {
       given: 'a blank data node that exist in db'
           def blankDataNode = new DataNodeBuilder().withDataspace(NCMP_DATASPACE_NAME)
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry').build()
       and: 'cps data service return an empty data node'
            mockCpsDataService.getDataNodes(*_) >> [blankDataNode]
       when: 'the yangModelSubscriptionEvent is saved into db'
            objectUnderTest.saveSubscriptionEvent(yangModelSubscriptionEvent)
       then: 'the cpsDataService save operation is called with the correct data'
            1 * mockCpsDataService.saveListElements(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PARENT,
                '{"subscription":[{' +
                    '"topic":"some-topic",' +
                    '"predicates":{"datastore":"some-datastore","targetCmHandles":[{"cmHandleId":"cmhandle1","status":"PENDING","details":"Subscription forwarded to dmi plugin"},' +
                    '{"cmHandleId":"cmhandle2","status":"PENDING","details":"Subscription forwarded to dmi plugin"}]},' +
                    '"clientID":"some-client-id","subscriptionName":"some-subscription-name","isTagged":true}]}',
                NO_TIMESTAMP)
   }

    def 'add or replace cm handle list element into db' () {
        given: 'a data node with child node exist in db'
            def leaves1 = [status:'REJECTED', cmHandleId:'cmhandle1', details:'Cm handle does not exist'] as Map
            def childDataNode = new DataNodeBuilder().withDataspace(NCMP_DATASPACE_NAME)
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription')
                .withLeaves(leaves1).build()
            def engagedDataNode = new DataNodeBuilder().withDataspace(NCMP_DATASPACE_NAME)
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry')
                .withChildDataNodes([childDataNode]).build()
        and: 'cps data service return data node including a child data node'
            mockCpsDataService.getDataNodes(*_) >> [engagedDataNode]
        and: 'cps data service return data node for querying by xpaths'
            mockCpsDataService.getDataNodesForMultipleXpaths(*_) >> [engagedDataNode]
        when: 'the yang model subscription event is saved into db'
            objectUnderTest.saveSubscriptionEvent(yangModelSubscriptionEvent)
        then: 'the cpsDataService save non-existing cm handle with the correct data'
            1 * mockCpsDataService.saveListElements(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PREDICATES_XPATH, '{"targetCmHandles":[{"cmHandleId":"cmhandle2","status":"PENDING","details":"Subscription forwarded to dmi plugin"}]}',
                NO_TIMESTAMP)
        and: 'the cpsDataService update existing cm handle with the correct data'
            1 * mockCpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PREDICATES_XPATH, '{"targetCmHandles":[{"cmHandleId":"cmhandle1","status":"PENDING","details":"Subscription forwarded to dmi plugin"}]}',
                NO_TIMESTAMP)
    }

}
