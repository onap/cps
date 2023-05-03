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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP

class SubscriptionPersistenceSpec extends Specification {

    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_PARENT = "/subscription-registry";

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new SubscriptionPersistenceImpl(jsonObjectMapper, mockCpsDataService)

   def 'save a subscription event' () {
       given: 'a yang model subscription event'
           def predicates = new YangModelSubscriptionEvent.Predicates(datastore: 'some-datastore',
               targetCmHandles: [new YangModelSubscriptionEvent.TargetCmHandle('cmhandle1'),
                                 new YangModelSubscriptionEvent.TargetCmHandle('cmhandle2')])
           def yangModelSubscriptionEvent = new YangModelSubscriptionEvent(clientId: 'some-client-id',
                subscriptionName: 'some-subscription-name', tagged: true, topic: 'some-topic', predicates: predicates)
       and: 'a data node that does not exist in db'
           def dataNodeNonExist = new DataNodeBuilder().withDataspace('NCMP-Admin')
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry').build()
       and: 'cps data service return non existing data node'
            mockCpsDataService.getDataNodes(*_) >> [dataNodeNonExist]
       when: 'the yangModelSubscriptionEvent is saved into db'
            objectUnderTest.saveSubscriptionEvent(yangModelSubscriptionEvent)
       then: 'the cpsDataService save operation is called with the correct data'
            1 * mockCpsDataService.saveListElements(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PARENT,
                '{"subscription":[{' +
                    '"topic":"some-topic",' +
                    '"predicates":{"datastore":"some-datastore","targetCmHandles":[{"cmHandleId":"cmhandle1","status":"PENDING"},{"cmHandleId":"cmhandle2","status":"PENDING"}]},' +
                    '"clientID":"some-client-id","subscriptionName":"some-subscription-name","isTagged":true}]}',
                NO_TIMESTAMP)
   }

    def 'update a subscription event' () {
        given: 'a yang model subscription event'
            def predicates = new YangModelSubscriptionEvent.Predicates(datastore: 'some-datastore',
                targetCmHandles: [new YangModelSubscriptionEvent.TargetCmHandle('cmhandle1'),
                                  new YangModelSubscriptionEvent.TargetCmHandle('cmhandle2')])
            def yangModelSubscriptionEvent = new YangModelSubscriptionEvent(clientId: 'some-client-id',
                subscriptionName: 'some-subscription-name', tagged: true, topic: 'some-topic', predicates: predicates)
        and: 'a data node exist in db'
            def childDataNode = new DataNodeBuilder().withDataspace('NCMP-Admin')
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription').build()
            def dataNodeExist = new DataNodeBuilder().withDataspace('NCMP-Admin')
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry')
                .withChildDataNodes([childDataNode]).build()
        and: 'cps data service return existing data node'
            mockCpsDataService.getDataNodes(*_) >> [dataNodeExist]
        when: 'the yangModelSubscriptionEvent is saved into db'
            objectUnderTest.saveSubscriptionEvent(yangModelSubscriptionEvent)
        then: 'the cpsDataService update operation is called with the correct data'
            1 * mockCpsDataService.updateDataNodeAndDescendants(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PARENT,
                '{"subscription":[{' +
                    '"topic":"some-topic",' +
                    '"predicates":{"datastore":"some-datastore","targetCmHandles":[{"cmHandleId":"cmhandle1","status":"PENDING"},{"cmHandleId":"cmhandle2","status":"PENDING"}]},' +
                    '"clientID":"some-client-id","subscriptionName":"some-subscription-name","isTagged":true}]}',
                NO_TIMESTAMP)
    }

}
