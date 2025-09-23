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


import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventMapper
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.EventProducer
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.impl.utils.JexParser
import spock.lang.Specification

class CmSubscriptionHandlerImplSpec extends Specification {

    def mockCmSubscriptionPersistenceService = Mock(CmDataJobSubscriptionPersistenceService)
    def mockDmiInEventMapper = Mock(DmiInEventMapper)
    def mockDmiInEventProducer = Mock(EventProducer)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)

    def objectUnderTest = new CmSubscriptionHandlerImpl(mockCmSubscriptionPersistenceService, mockDmiInEventMapper,
        mockDmiInEventProducer, mockInventoryPersistence, mockAlternateIdMatcher)

    def 'Process subscription CREATE request for new target [non existing]'() {
        given: 'relevant subscription details'
            def mySubId = 'dataJobId'
            def myDataNodeSelectors = ['/parent[id="1"]'].toList()
            def notificationTypes = []
            def notificationFilter = ''
            def dataSelector = new DataSelector(notificationTypes: notificationTypes, notificationFilter: notificationFilter)
        and: 'alternate Id matcher returns cm handle id for given data node selector'
            def fdn = getFdn(myDataNodeSelectors.iterator().next())
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'myCmHandleId'
        and: 'returns inactive data node selector(s)'
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(mySubId) >> ['/parent[id="1"]']
        and: 'the inventory persistence service returns cm handle'
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId') >> new YangModelCmHandle(dmiServiceName: 'myDmiService')
        and: 'DMI in event mapper returns event'
            def myDmiInEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId'], myDataNodeSelectors, notificationTypes, notificationFilter) >> myDmiInEvent
        when: 'the method to process subscription create request is called'
            objectUnderTest.processSubscriptionCreate(dataSelector, mySubId, myDataNodeSelectors)
        then: 'the persistence service is called'
            1 * mockCmSubscriptionPersistenceService.add(mySubId, '/parent[id="1"]')
        and: 'the event is sent to correct DMI'
            1 * mockDmiInEventProducer.send(mySubId, 'myDmiService', 'subscriptionCreateRequest', _)
    }

    def 'Process subscription CREATE request for new targets [non existing] to be sent to multiple DMIs'() {
        given: 'relevant subscription details'
            def mySubId = 'dataJobId'
            def myDataNodeSelectors = [
                '/parent[id="forDmi1"]',
                '/parent[id="forDmi1"]/child',
                '/parent[id="forDmi2"]'].toList()
            def someAttr1 = []
            def someAttr2 = ''
            def dataSelector = new DataSelector(notificationTypes: someAttr1, notificationFilter: someAttr2)
        and: 'alternate Id matcher returns cm handle ids for given data node selectors'
            def fdn1 = getFdn(myDataNodeSelectors.get(0))
            def fdn2 = getFdn(myDataNodeSelectors.get(1))
            def fdn3 = getFdn(myDataNodeSelectors.get(2))
            mockAlternateIdMatcher.getCmHandleId(fdn1) >> 'myCmHandleId1'
            mockAlternateIdMatcher.getCmHandleId(fdn2) >> 'myCmHandleId1'
            mockAlternateIdMatcher.getCmHandleId(fdn3) >> 'myCmHandleId2'
        and: 'returns inactive data node selector(s)'
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(mySubId) >> [
                '/parent[id="forDmi1"]',
                '/parent[id="forDmi1"]/child',
                '/parent[id="forDmi2"]']
        and: 'the inventory persistence service returns cm handles with dmi information'
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId1') >> new YangModelCmHandle(dmiServiceName: 'myDmiService1')
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId2') >> new YangModelCmHandle(dmiServiceName: 'myDmiService2')
        and: 'DMI in event mapper returns events'
            def myDmiInEvent1 = new DataJobSubscriptionDmiInEvent()
            def myDmiInEvent2 = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId1'], ['/parent[id="forDmi1"]', '/parent[id="forDmi1"]/child'], someAttr1, someAttr2) >> myDmiInEvent1
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId2'], ['/parent[id="forDmi2"]'], someAttr1, someAttr2) >> myDmiInEvent2
        when: 'the method to process subscription create request is called'
            objectUnderTest.processSubscriptionCreate(dataSelector, mySubId, myDataNodeSelectors)
        then: 'the persistence service is called'
            myDataNodeSelectors.each { dataNodeSelector ->
                1 * mockCmSubscriptionPersistenceService.add(_, dataNodeSelector)}
        and: 'the event is sent to correct DMIs'
            1 * mockDmiInEventProducer.send(mySubId, 'myDmiService1', 'subscriptionCreateRequest', myDmiInEvent1)
            1 * mockDmiInEventProducer.send(mySubId, 'myDmiService2', 'subscriptionCreateRequest', myDmiInEvent2)
    }

    def 'Process subscription CREATE request for overlapping targets [non existing & existing]'() {
        given: 'relevant subscription details'
            def myNewSubId = 'newId'
            def myDataNodeSelectors = ['/newDataNodeSelector[id=""]'].toList()
            def dataSelector = new DataSelector(notificationTypes: [], notificationFilter: '')
        and: 'alternate id matcher always returns a cm handle id'
            mockAlternateIdMatcher.getCmHandleId(_) >> 'someCmHandleId'
        and: 'the inventory persistence service returns cm handles with dmi information'
            mockInventoryPersistence.getYangModelCmHandle(_) >> new YangModelCmHandle(dmiServiceName: 'myDmiService')
        and: 'returns inactive data node selector(s)'
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(myNewSubId) >> inactiveDataNodeSelectors
        when: 'the method to process subscription create request is called'
            objectUnderTest.processSubscriptionCreate(dataSelector, myNewSubId, myDataNodeSelectors)
        then: 'the persistence service is called'
            1 * mockCmSubscriptionPersistenceService.add(_, myDataNodeSelectors.iterator().next())
        and: 'the event is sent to correct DMIs'
            expectedCallsToDmi * mockDmiInEventProducer.send(myNewSubId, 'myDmiService', 'subscriptionCreateRequest', _)
        where: 'following data are used'
            scenario                                           | inactiveDataNodeSelectors                                         || expectedCallsToDmi
            'new target overlaps with ACCEPTED targets'        | []                                                                || 0
            'new target overlaps with REJECTED targets'        | ['/existingDataNodeSelector[id=""]','/newDataNodeSelector[id=""]']|| 1
            'new target overlaps with UNKNOWN targets'         | ['/existingDataNodeSelector[id=""]','/newDataNodeSelector[id=""]']|| 1
            'new target does not overlap with existing targets'| ['/newDataNodeSelector[id=""]']                                   || 1
    }

    def 'Process subscription DELETE request where all selectors become unused'() {
        given: 'a subscription id and its associated selectors'
            def mySubId = 'deleteJobId'
            def selectors = ['/node[id="1"]']
            mockCmSubscriptionPersistenceService.getDataNodeSelectorsBySubscriptionId(mySubId) >> selectors
        and: 'no other subscriptions exist for the selectors'
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="1"]') >> []
        and: 'cm handle resolution setup'
            def fdn = getFdn('/node[id="1"]')
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'cmHandleId1'
            mockInventoryPersistence.getYangModelCmHandle('cmHandleId1') >> new YangModelCmHandle(dmiServiceName: 'dmiService1')
        and: 'mapper returns delete event'
            def deleteEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInDeleteEvent(['cmHandleId1'], ['/node[id="1"]']) >> deleteEvent
        when: 'the subscription delete request is processed'
            objectUnderTest.processSubscriptionDelete(mySubId)
        then: 'subscription is removed from persistence'
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="1"]')
        and: 'status is updated to unknown'
            1 * mockCmSubscriptionPersistenceService.updateStatus('/node[id="1"]', 'unknown')
        and: 'delete event is sent to correct DMI'
            1 * mockDmiInEventProducer.send(mySubId, 'dmiService1', 'subscriptionDeleteRequest', deleteEvent)
    }

    def 'Process subscription DELETE request where some selectors are still in use'() {
        given: 'a subscription id and two associated selectors'
            def mySubId = 'deleteJobId2'
            def selectors = ['/node[id="1"]','/node[id="2"]']
            mockCmSubscriptionPersistenceService.getDataNodeSelectorsBySubscriptionId(mySubId) >> selectors
        and: 'selector 1 has no more subscribers, selector 2 still has subscribers'
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="1"]') >> []
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="2"]') >> ['anotherSub']
        and: 'cm handle resolution for selector 1'
            def fdn = getFdn('/node[id="1"]')
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'cmHandleIdX'
            mockInventoryPersistence.getYangModelCmHandle('cmHandleIdX') >> new YangModelCmHandle(dmiServiceName: 'dmiServiceX')
        and: 'mapper returns delete event'
            def deleteEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInDeleteEvent(['cmHandleIdX'], ['/node[id="1"]']) >> deleteEvent
        when: 'the subscription delete request is processed'
            objectUnderTest.processSubscriptionDelete(mySubId)
        then: 'subscription is removed from persistence for both selectors'
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="1"]')
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="2"]')
        and: 'status is updated only for selector 1'
            1 * mockCmSubscriptionPersistenceService.updateStatus('/node[id="1"]', 'unknown')
            0 * mockCmSubscriptionPersistenceService.updateStatus('/node[id="2"]', _)
        and: 'delete event is sent only for selector 1'
            1 * mockDmiInEventProducer.send(mySubId, 'dmiServiceX', 'subscriptionDeleteRequest', deleteEvent)
    }

    def 'Process subscription DELETE request where cmHandleId cannot be resolved'() {
        given: 'a subscription id and its selector'
            def mySubId = 'deleteJobId3'
            def selectors = ['/node[id="unresolvable"]']
            mockCmSubscriptionPersistenceService.getDataNodeSelectorsBySubscriptionId(mySubId) >> selectors
        and: 'no more subscriptions exist for the selector'
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="unresolvable"]') >> []
        and: 'alternate id matcher cannot resolve cm handle id'
            def fdn = getFdn('/node[id="unresolvable"]')
            mockAlternateIdMatcher.getCmHandleId(fdn) >> null
        when: 'the subscription delete request is processed'
            objectUnderTest.processSubscriptionDelete(mySubId)
        then: 'subscription is removed from persistence'
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="unresolvable"]')
        and: 'status is updated to unknown'
            1 * mockCmSubscriptionPersistenceService.updateStatus('/node[id="unresolvable"]', 'unknown')
        and: 'no delete event is sent because cmHandleId was not resolved'
            0 * mockDmiInEventProducer.send(_, _, _, _)
    }

    def getFdn(dataNodeSelector) {
        return JexParser.extractFdnPrefix(dataNodeSelector).orElse("")
    }
}
