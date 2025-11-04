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

import ch.qos.logback.classic.Level

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.ACCEPTED
import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.REJECTED

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventMapper
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.EventProducer
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.impl.utils.JexParser
import org.slf4j.LoggerFactory
import spock.lang.Specification

class CmSubscriptionHandlerImplSpec extends Specification {

    def mockCmSubscriptionPersistenceService = Mock(CmDataJobSubscriptionPersistenceService)
    def mockDmiInEventMapper = Mock(DmiInEventMapper)
    def mockDmiInEventProducer = Mock(EventProducer)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)
    def logger = new ListAppender<ILoggingEvent>()

    void setup() {
        ((Logger) LoggerFactory.getLogger(CmSubscriptionHandlerImpl.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmSubscriptionHandlerImpl.class)).detachAndStopAllAppenders()
    }

    def objectUnderTest = new CmSubscriptionHandlerImpl(mockCmSubscriptionPersistenceService, mockDmiInEventMapper,
            mockDmiInEventProducer, mockInventoryPersistence, mockAlternateIdMatcher)

    def 'Ignore CREATE request for existing subscription id'() {
        given: 'an existing subscription id'
            mockCmSubscriptionPersistenceService.isNewSubscriptionId('existingId') >> false
        when: 'a subscription is created'
            objectUnderTest.createSubscription(new DataSelector(), 'existingId', ['/someDataNodeSelector'])
        then: 'request is ignored and no method is invoked'
            0 * mockCmSubscriptionPersistenceService.add(*_)
        and: 'an event is sent to the correct DMI'
            0 * mockDmiInEventProducer.send(*_)
    }

    def 'Process subscription CREATE request for new target [non existing]'() {
        given: 'relevant subscription details'
            def mySubId = 'dataJobId'
            def myDataNodeSelectors = ['/parent[id="1"]'].toList()
            def notificationTypes = []
            def notificationFilter = ''
            def dataSelector = new DataSelector(notificationTypes: notificationTypes, notificationFilter: notificationFilter)
        and: 'persistence service confirms subscription id is new'
            mockCmSubscriptionPersistenceService.isNewSubscriptionId(mySubId) >> true
        and: 'alternate Id matcher returns cm handle id for given data node selector'
            def fdn = getFdn(myDataNodeSelectors.iterator().next())
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'myCmHandleId'
        and: 'the persistence service returns inactive data node selector(s)'
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(mySubId) >> ['/parent[id="1"]']
        and: 'the inventory persistence service returns cm handle'
            mockInventoryPersistence.getYangModelCmHandle('myCmHandleId') >> new YangModelCmHandle(dmiServiceName: 'myDmiService')
        and: 'DMI in event mapper returns event'
            def myDmiInEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId'], myDataNodeSelectors, notificationTypes, notificationFilter) >> myDmiInEvent
        when: 'a subscription is created'
            objectUnderTest.createSubscription(dataSelector, mySubId, myDataNodeSelectors)
        then: 'each datanode selector is added using the persistence service'
            1 * mockCmSubscriptionPersistenceService.add(mySubId, '/parent[id="1"]')
        and: 'an event is sent to the correct DMI'
            1 * mockDmiInEventProducer.send(mySubId, 'myDmiService', 'subscriptionCreateRequest', _)
    }

    def 'Process subscription CREATE request for new targets [non existing] to be sent to multiple DMIs'() {
        given: 'relevant subscription details'
            def mySubId = 'dataJobId'
            def myDataNodeSelectors = [
                    '/parent[id="forDmi1"]',
                    '/parent[id="forDmi1"]/child',
                    '/parent[id="forDmi2"]'].toList()
            def notificationTypes = []
            def notificationFilter = ''
            def dataSelector = new DataSelector(notificationTypes: notificationTypes, notificationFilter: notificationFilter)
        and: 'persistence service confirms subscription id is new'
            mockCmSubscriptionPersistenceService.isNewSubscriptionId(mySubId) >> true
        and: 'alternate Id matcher returns cm handle ids for given data node selectors'
            def fdn1 = getFdn(myDataNodeSelectors[0])
            def fdn2 = getFdn(myDataNodeSelectors[1])
            def fdn3 = getFdn(myDataNodeSelectors[2])
            mockAlternateIdMatcher.getCmHandleId(fdn1) >> 'myCmHandleId1'
            mockAlternateIdMatcher.getCmHandleId(fdn2) >> 'myCmHandleId1'
            mockAlternateIdMatcher.getCmHandleId(fdn3) >> 'myCmHandleId2'
        and: 'the persistence service returns inactive data node selector(s)'
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
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId1'], ['/parent[id="forDmi1"]', '/parent[id="forDmi1"]/child'], notificationTypes, notificationFilter) >> myDmiInEvent1
            mockDmiInEventMapper.toDmiInEvent(['myCmHandleId2'], ['/parent[id="forDmi2"]'], notificationTypes, notificationFilter) >> myDmiInEvent2
        when: 'a subscription is created'
            objectUnderTest.createSubscription(dataSelector, mySubId, myDataNodeSelectors)
        then: 'each datanode selector is added using the persistence service'
            myDataNodeSelectors.each { dataNodeSelector ->
                1 * mockCmSubscriptionPersistenceService.add(_, dataNodeSelector)
            }
        and: 'an event is sent to each DMI involved'
            1 * mockDmiInEventProducer.send(mySubId, 'myDmiService1', 'subscriptionCreateRequest', myDmiInEvent1)
            1 * mockDmiInEventProducer.send(mySubId, 'myDmiService2', 'subscriptionCreateRequest', myDmiInEvent2)
    }

    def 'Process subscription CREATE request for overlapping targets [non existing & existing]'() {
        given: 'relevant subscription details'
            def myNewSubId = 'newId'
            def myDataNodeSelectors = ['/newDataNodeSelector[id=""]'].toList()
            def dataSelector = new DataSelector(notificationTypes: [], notificationFilter: '')
        and: 'persistence service confirms subscription id is new'
            mockCmSubscriptionPersistenceService.isNewSubscriptionId(myNewSubId) >> true
        and: 'alternate id matcher always returns a cm handle id'
            mockAlternateIdMatcher.getCmHandleId(_) >> 'someCmHandleId'
        and: 'the inventory persistence service returns cm handles with dmi information'
            mockInventoryPersistence.getYangModelCmHandle(_) >> new YangModelCmHandle(dmiServiceName: 'myDmiService')
        and: 'the inventory persistence service returns inactive data node selector(s)'
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(myNewSubId) >> inactiveDataNodeSelectors
        when: 'a subscription is created'
            objectUnderTest.createSubscription(dataSelector, myNewSubId, myDataNodeSelectors)
        then: 'each datanode selector is added using the persistence service'
            1 * mockCmSubscriptionPersistenceService.add(_, myDataNodeSelectors.iterator().next())
        and: 'an event is sent to each DMI involved'
            expectedCallsToDmi * mockDmiInEventProducer.send(myNewSubId, 'myDmiService', 'subscriptionCreateRequest', _)
        where: 'following data are used'
            scenario                                            | inactiveDataNodeSelectors                                           || expectedCallsToDmi
            'new target overlaps with ACCEPTED targets'         | []                                                                  || 0
            'new target overlaps with REJECTED targets'         | ['/existingDataNodeSelector[id=""]', '/newDataNodeSelector[id=""]'] || 1
            'new target overlaps with UNKNOWN targets'          | ['/existingDataNodeSelector[id=""]', '/newDataNodeSelector[id=""]'] || 1
            'new target does not overlap with existing targets' | ['/newDataNodeSelector[id=""]']                                     || 1
    }

    def 'Process subscription DELETE request where all data node selectors become unused'() {
        given: 'a subscription id and its associated data node selectors'
            def mySubId = 'deleteJobId'
            def myDataNodeSelector = ['/node[id="1"]']
        and: 'the persistence service returns the data node selectors'
            mockCmSubscriptionPersistenceService.getDataNodeSelectors(mySubId) >> myDataNodeSelector
        and: 'no other subscriptions exist for the data node selectors'
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="1"]') >> []
        and: 'cm handle resolution setup'
            def fdn = getFdn('/node[id="1"]')
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'cmHandleId1'
            mockInventoryPersistence.getYangModelCmHandle('cmHandleId1') >> new YangModelCmHandle(dmiServiceName: 'dmiService1')
        and: 'DMI in event mapper returns events'
            def deleteEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['cmHandleId1'], ['/node[id="1"]'], null, null) >> deleteEvent
        when: 'a subscription is deleted'
            objectUnderTest.deleteSubscription(mySubId)
        then: 'subscription is removed from persistence'
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="1"]')
        and: 'an event is sent to each DMI involved'
            1 * mockDmiInEventProducer.send(mySubId, 'dmiService1', 'subscriptionDeleteRequest', deleteEvent)
    }

    def 'Process subscription DELETE request where some data node selectors are still in use'() {
        given: 'a subscription id and two associated selectors'
            def mySubId = 'deleteJobId2'
            def dataNodeSelectors = ['/node[id="1"]', '/node[id="2"]']
        and: 'the persistence service returns the data node selectors'
            mockCmSubscriptionPersistenceService.getDataNodeSelectors(mySubId) >> dataNodeSelectors
        and: 'data node selector 1 has no more subscribers, data node selector 2 still has subscribers'
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="1"]') >> []
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="2"]') >> ['anotherSub']
        and: 'cm handle resolution for data node selector 1'
            def fdn = getFdn('/node[id="1"]')
            mockAlternateIdMatcher.getCmHandleId(fdn) >> 'cmHandleIdX'
            mockInventoryPersistence.getYangModelCmHandle('cmHandleIdX') >> new YangModelCmHandle(dmiServiceName: 'dmiServiceX')
        and: 'DMI in event mapper returns events'
            def deleteEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['cmHandleIdX'], ['/node[id="1"]'], null, null) >> deleteEvent
        when: 'a subscription is deleted'
            objectUnderTest.deleteSubscription(mySubId)
        then: 'subscription is removed from persistence for both data node selectors'
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="1"]')
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="2"]')
        and: 'delete event is sent only for data node selectors without any subscriber'
            1 * mockDmiInEventProducer.send(mySubId, 'dmiServiceX', 'subscriptionDeleteRequest', deleteEvent)
    }

    def 'Process subscription DELETE request where cmHandleId cannot be resolved'() {
        given: 'a subscription id and its data node selector'
            def mySubId = 'deleteJobId3'
            def dataNodeSelectors = ['/node[id="unresolvable"]']
        and: 'the persistence service returns the data node selectors'
            mockCmSubscriptionPersistenceService.getDataNodeSelectors(mySubId) >> dataNodeSelectors
        and: 'no more subscriptions exist for the data node selector'
            mockCmSubscriptionPersistenceService.getSubscriptionIds('/node[id="unresolvable"]') >> []
        and: 'alternate id matcher cannot resolve cm handle id'
            def fdn = getFdn('/node[id="unresolvable"]')
            mockAlternateIdMatcher.getCmHandleId(fdn) >> null
        and: 'DMI in event mapper returns events'
            def deleteEvent = new DataJobSubscriptionDmiInEvent()
            mockDmiInEventMapper.toDmiInEvent(['cmHandleIdX'], ['/node[id="1"]'], null, null) >> deleteEvent
        when: 'a subscription is deleted'
            objectUnderTest.deleteSubscription(mySubId)
        then: 'subscription is removed from persistence'
            1 * mockCmSubscriptionPersistenceService.delete(mySubId, '/node[id="unresolvable"]')
        and: 'no delete event is sent because cmHandleId was not resolved'
            0 * mockDmiInEventProducer.send(*_)
    }

    def 'Update subscription status to ACCEPTED: #scenario'() {
        given: 'a subscription ID'
            def mySubscriptionId = 'mySubId'
        and: 'the persistence service returns all inactive data node selectors'
            def myDataNodeSelectors = ['/myDataNodeSelector[id=""]'].asList()
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(mySubscriptionId) >> myDataNodeSelectors
        and: 'alternate id matcher always returns a cm handle id'
            mockAlternateIdMatcher.getCmHandleId(_) >> 'someCmHandleId'
        and: 'the inventory persistence service returns a yang model with a dmi service name for the accepted subscription'
            mockInventoryPersistence.getYangModelCmHandle(_) >> new YangModelCmHandle(dmiServiceName: 'myDmi')
        when: 'the method to update subscription status is called with status=ACCEPTED and dmi #dmiName'
            objectUnderTest.updateCmSubscriptionStatus(mySubscriptionId, dmiName, ACCEPTED)
        then: 'the persistence service to update subscription status is ONLY called for matching dmi name'
            expectedCallsToPersistenceService * mockCmSubscriptionPersistenceService.updateCmSubscriptionStatus('/myDataNodeSelector[id=""]', ACCEPTED)
        where: 'the following data are used'
            scenario                           | dmiName        || expectedCallsToPersistenceService
            'data node selector for "myDmi"'   | 'myDmi'        || 1
            'data node selector for other dmi' | 'someOtherDmi' || 0
    }

    def 'Log update when subscription status is REJECTED'() {
        given: 'dmi service name and subscription id'
            def myDmi = 'myDmi'
            def mySubscriptionId = 'mySubscriptionId'
        and: 'the persistence service returns all inactive data node selectors'
            def myDataNodeSelectors = ['/parent[id=""]'].asList()
            mockCmSubscriptionPersistenceService.getInactiveDataNodeSelectors(mySubscriptionId) >> myDataNodeSelectors
        and: 'alternate id matcher always returns a cm handle id'
            mockAlternateIdMatcher.getCmHandleId(_) >> 'someCmHandleId'
        and: 'the inventory persistence service returns a yang model with the given dmi service name'
            mockInventoryPersistence.getYangModelCmHandle(_) >> new YangModelCmHandle(dmiServiceName: myDmi)
        when: 'update subscription status is called with status=REJECTED'
            objectUnderTest.updateCmSubscriptionStatus(mySubscriptionId, myDmi, REJECTED)
        then: 'the persistence service to update subscription status called with REJECTED for matching dmi name'
            1 * mockCmSubscriptionPersistenceService.updateCmSubscriptionStatus('/parent[id=""]', REJECTED)
        and: 'an event is logged with level INFO'
            def loggingEvent = logger.list[0]
            assert loggingEvent.level == Level.INFO
        and: 'the log contains details of the rejected data node selectors'
            assert loggingEvent.formattedMessage == 'DataJob CREATE request with the following details was rejected by DMI plugin myDmi: dataJobId=mySubscriptionId | dataNodeSelector=/parent[id=""]'

    }


    def getFdn(dataNodeSelector) {
        return JexParser.extractFdnPrefix(dataNodeSelector).orElse("")
    }
}