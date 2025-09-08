/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.onap.cps.ncmp.impl.datajobs.subscription.ncmp

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.UNKNOWN;

import org.onap.cps.api.model.DataNode
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventMapper
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventProducer
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.impl.utils.JexParser
import spock.lang.Specification

class CmSubscriptionHandlerImplSpec extends Specification {

    def mockCmSubscriptionPersistenceService = Mock(CmDataJobSubscriptionPersistenceService)
    def mockDmiInEventMapper = Mock(DmiInEventMapper)
    def mockDmiInEventProducer = Mock(DmiInEventProducer)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)

    def objectUnderTest = new CmSubscriptionHandlerImpl(mockCmSubscriptionPersistenceService, mockDmiInEventMapper,
        mockDmiInEventProducer, mockInventoryPersistence, mockAlternateIdMatcher)

    def 'Process subscription CREATE request for new target [non existing]'() {
        given: 'relevant subscription details'
            def mySubId = 'dataJobId'
            def myDataNodeSelectors = ['/parent[id="1"]'].toList()
            def someAttr1 = []
            def someAttr2 = ''
            def dataJobExtraAttr = new DataSelector(notificationTypes: someAttr1, notificationFilter: someAttr2)
        and: 'alternate Id matcher returns cm handle id for given data node selector'
            def fdn = getFdn(myDataNodeSelectors.iterator().next())
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'myCmHandleId'
        and: 'returns NO data node selectors with rejected status'
            mockCmSubscriptionPersistenceService.getRejectedDataNodeSelectors(mySubId) >> []
        and: 'returns data node with unknown status'
            mockCmSubscriptionPersistenceService.getDataNodesForSubscription(mySubId, UNKNOWN)
                >> [new DataNode(leaves: ['dataJobId': ['dataJobId'], 'dataNodeSelector': '/parent[id="1"]'])]
        and: 'the inventory persistence service returns cm handle'
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId') >> new YangModelCmHandle(dmiServiceName: 'myDmiService')
        and: 'DMI in event mapper returns event'
            def myDmiInEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId'], myDataNodeSelectors, someAttr1, someAttr2) >> myDmiInEvent
        when: 'the method to process subscription create request is called'
            objectUnderTest.processSubscriptionCreateRequest(mySubId, myDataNodeSelectors, dataJobExtraAttr)
        then: 'the persistence service is called'
            1 * mockCmSubscriptionPersistenceService.addSubscription(_, mySubId)
        and: 'the event is sent to correct DMI'
            1 * mockDmiInEventProducer.sendDmiInEvent(mySubId, 'myDmiService', 'subscriptionCreateRequest', _)
    }

    def 'Process subscription CREATE request for new targets [non existing] to be sent to multiple DMIs'() {
        given: 'relevant subscription details'
            def mySubId = 'dataJobId'
            def myDataNodeSelectors = ['/parent[id="forDmi1"]',
                                       '/parent[id="forDmi1"]/child',
                                       '/parent[id="forDmi2"]'].toList()
            def someAttr1 = []
            def someAttr2 = ''
            def dataJobExtraAttr = new DataSelector(notificationTypes: someAttr1, notificationFilter: someAttr2)
        and: 'alternate Id matcher returns cm handle ids for given data node selectors'
            def fdn1 = getFdn(myDataNodeSelectors.get(0))
            def fdn2 = getFdn(myDataNodeSelectors.get(1))
            def fdn3 = getFdn(myDataNodeSelectors.get(2))
            mockAlternateIdMatcher.getCmHandleId(fdn1) >> 'myCmHandleId1'
            mockAlternateIdMatcher.getCmHandleId(fdn2) >> 'myCmHandleId1'
            mockAlternateIdMatcher.getCmHandleId(fdn3) >> 'myCmHandleId2'
        and: 'returns NO data node selectors with rejected status'
            mockCmSubscriptionPersistenceService.getRejectedDataNodeSelectors(_) >> []
        and: 'returns data node with unknown status'
            mockCmSubscriptionPersistenceService.getDataNodesForSubscription(_, UNKNOWN)
                >> [(new DataNode(leaves: ['dataJobId': [mySubId], 'dataNodeSelector': '/parent[id="forDmi1"]'])),
                    (new DataNode(leaves: ['dataJobId': [mySubId], 'dataNodeSelector': '/parent[id="forDmi1"]/child'])),
                    (new DataNode(leaves: ['dataJobId': [mySubId], 'dataNodeSelector': '/parent[id="forDmi2"]']))]
        and: 'the inventory persistence service returns cm handles with dmi information'
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId1') >> new YangModelCmHandle(dmiServiceName: 'myDmiService1')
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId2') >> new YangModelCmHandle(dmiServiceName: 'myDmiService2')
        and: 'DMI in event mapper returns events'
            def myDmiInEvent1 = new DataJobSubscriptionDmiInEvent()
            def myDmiInEvent2 = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId1'], ['/parent[id="forDmi1"]', '/parent[id="forDmi1"]/child'], someAttr1, someAttr2) >> myDmiInEvent1
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId2'], ['/parent[id="forDmi2"]'], someAttr1, someAttr2) >> myDmiInEvent2
        when: 'the method to process subscription create request is called'
            objectUnderTest.processSubscriptionCreateRequest(mySubId, myDataNodeSelectors, dataJobExtraAttr)
        then: 'the persistence service is called'
            myDataNodeSelectors.each { dataNodeSelector ->
                1 * mockCmSubscriptionPersistenceService.addSubscription(dataNodeSelector, _)}
        and: 'the event is sent to correct DMIs'
            1 * mockDmiInEventProducer.sendDmiInEvent(mySubId, 'myDmiService1', 'subscriptionCreateRequest', myDmiInEvent1)
            1 * mockDmiInEventProducer.sendDmiInEvent(mySubId, 'myDmiService2', 'subscriptionCreateRequest', myDmiInEvent2)
    }

    def 'Process subscription CREATE request for overlapping targets [non existing & existing]'() {
        given: 'relevant subscription details'
            def myNewSubId = 'newId'
            def myDataNodeSelectors = ['/newDataNodeSelector'].toList()
            def dataJobExtraAttr = new DataSelector(notificationTypes: [], notificationFilter: '')
        and: 'alternate Id matcher returns cm handle ids for given data node selectors'
            def fdn1 = getFdn(myDataNodeSelectors.get(0))
            mockAlternateIdMatcher.getCmHandleId(fdn1) >> 'myCmHandleId1'
        and: 'the inventory persistence service returns cm handles with dmi information'
            mockInventoryPersistence.getYangModelCmHandle(_) >> new YangModelCmHandle(dmiServiceName: 'myDmiService')
        and: 'returns NO data node selectors with rejected status'
            mockCmSubscriptionPersistenceService.getRejectedDataNodeSelectors(_) >> rejectedDataNodeSelectorList
        and: 'returns data nodes with unknown status'
            mockCmSubscriptionPersistenceService.getDataNodesForSubscription(_, UNKNOWN) >> unknownDataNodes
        when: 'the method to process subscription create request is called'
            objectUnderTest.processSubscriptionCreateRequest(myNewSubId, myDataNodeSelectors, dataJobExtraAttr)
        then: 'the persistence service is called'
            1 * mockCmSubscriptionPersistenceService.addSubscription(myDataNodeSelectors.iterator().next(), _)
        and: 'the event is sent to correct DMIs'
            expectedCallsToDmi * mockDmiInEventProducer.sendDmiInEvent(myNewSubId, 'myDmiService', 'subscriptionCreateRequest', _)
        where: 'following data are used'
            scenario                                           | rejectedDataNodeSelectorList | unknownDataNodes                                              || expectedCallsToDmi
            'new target overlaps with ACCEPTED targets'        | []                           | []                                                            || 0
            'new target overlaps with REJECTED targets'        | ['/existingDataNodeSelector']| []                                                            || 1
            'new target overlaps with UNKNOWN targets'         | []                           | [getDataNode(['newId', 'oldId'], '/existingDataNodeSelector')]|| 0
            'new target does not overlap with existing targets'| []                           | [getDataNode(['newId'], '/newDataNodeSelector')]              || 1
    }

    def getDataNode(dataJobIds, dataNodeSelector) {
        return (new DataNode(leaves: ['dataJobId': dataJobIds, 'dataNodeSelector': dataNodeSelector]))
    }

    def getFdn(dataNodeSelector) {
        return JexParser.extractFdnPrefix(dataNodeSelector).orElse("")
    }
}
