/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service

import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.ncmp.api.impl.operations.DatastoreType
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import java.time.OffsetDateTime

class CmNotificationSubscriptionPersistenceServiceImplSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new CmNotificationSubscriptionPersistenceServiceImpl(jsonObjectMapper, mockCpsQueryService, mockCpsDataService)

    def 'Check ongoing cm subscription #scenario'() {
        given: 'a valid cm subscription query'
            def cpsPathQuery = "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-1']/filters/filter[@xpath='/cps/path']";
        and: 'datanodes optionally returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                    cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >> dataNode
        when: 'we check for an ongoing cm subscription'
            def response = objectUnderTest.isOngoingCmNotificationSubscription(DatastoreType.PASSTHROUGH_RUNNING, 'ch-1', '/cps/path')
        then: 'we get expected response'
            assert response == isOngoingCmSubscription
        where: 'following scenarios are used'
            scenario                  | dataNode                                                                            || isOngoingCmSubscription
            'valid datanodes present' | [new DataNode(xpath: '/cps/path', leaves: ['subscriptionIds': ['sub-1', 'sub-2']])] || true
            'no datanodes present'    | []                                                                                  || false
    }

    def 'Checking uniqueness of incoming subscription ID'() {
        given: 'a cps path with a subscription ID for querying'
            def cpsPathQuery = objectUnderTest.SUBSCRIPTION_IDS_CPS_PATH_QUERY.formatted('some-sub')
        and: 'relevant datanodes are returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions', cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >>
                    dataNodes
        when: 'a subscription ID is tested for uniqueness'
            def result = objectUnderTest.isUniqueSubscriptionId('some-sub')
        then: 'result is as expected'
            assert result == isValidSubscriptionId
        where: 'following scenarios are used'
            scenario               | dataNodes        || isValidSubscriptionId
            'datanodes present'    | [new DataNode()] || false
            'no datanodes present' | []               || true
    }

    def 'Add new subscriber to an ongoing cm notification subscription'() {
        given: 'a valid cm subscription path query'
            def cpsPathQuery = objectUnderTest.CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted('ncmp-datastore:passthrough-running', 'ch-1', '/x/y')
        and: 'a dataNode exists for the given cps path query'
             mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >> [new DataNode(xpath: cpsPathQuery, leaves: ['xpath': '/x/y','subscriptionIds': ['sub-1']])]
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmNotificationSubscription(DatastoreType.PASSTHROUGH_RUNNING, 'ch-1','/x/y', 'newSubId')
        then: 'data service method to update list of subscribers is called once'
            1 * mockCpsDataService.updateNodeLeaves(
                'NCMP-Admin',
                'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']/filters',
                '{"filter":[{"xpath":"/x/y","subscriptionIds":["sub-1","newSubId"]}]}', _)
    }

    def 'Add new cm notification subscription for #datastoreType'() {
        given: 'a valid cm subscription path query'
            def cpsPathQuery = objectUnderTest.CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted(datastoreName, 'ch-1', '/x/y')
        and: 'a parent node xpath for given path above'
            def parentNodeXpath = '/datastores/datastore[@name=\'%s\']/cm-handles'
        and: 'a datanode does not exist for the given cps path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery.formatted(datastoreName),
                FetchDescendantsOption.OMIT_DESCENDANTS) >> []
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmNotificationSubscription(datastoreType, 'ch-1','/x/y', 'newSubId')
        then: 'data service method to update list of subscribers is called once with the correct parameters'
            1 * mockCpsDataService.saveData(
                'NCMP-Admin',
                'cm-data-subscriptions',
                parentNodeXpath.formatted(datastoreName),
                '{"cm-handle":[{"id":"ch-1","filters":{"filter":[{"xpath":"/x/y","subscriptionIds":["newSubId"]}]}}]}', _,_)
        where:
            scenario                  | datastoreType                          || datastoreName
            'passthrough_running'     | DatastoreType.PASSTHROUGH_RUNNING      || "ncmp-datastore:passthrough-running"
            'passthrough_operational' | DatastoreType.PASSTHROUGH_OPERATIONAL  || "ncmp-datastore:passthrough-operational"
    }

    def 'Remove subscriber from a list of an ongoing cm notification subscription'() {
        given: 'a subscription exists when queried'
            def cpsPathQuery = objectUnderTest.CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted('ncmp-datastore:passthrough-running', 'ch-1', '/x/y')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >> [new DataNode(xpath: cpsPathQuery, leaves: ['xpath': '/x/y','subscriptionIds': ['sub-1', 'sub-2']])]
        when: 'the subscriber is removed'
            objectUnderTest.removeCmNotificationSubscription(DatastoreType.PASSTHROUGH_RUNNING, 'ch-1', '/x/y', 'sub-1')
        then: 'the list of subscribers is updated'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']/filters',
                '{"filter":[{"xpath":"/x/y","subscriptionIds":["sub-2"]}]}', _)
    }

    def 'Removing ongoing subscription with no subscribers'(){
        given: 'a subscription exists when queried but has no subscribers'
            def cpsPathQuery = objectUnderTest.CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted('ncmp-datastore:passthrough-running', 'ch-1', '/x/y')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >> [new DataNode(xpath: cpsPathQuery, leaves: ['xpath': '/x/y','subscriptionIds': []])]
        when: 'a an ongoing subscription is refreshed'
            objectUnderTest.refreshOngoingCmNotificationSubscriptions(DatastoreType.PASSTHROUGH_RUNNING, 'ch-1', '/x/y')
        then: 'the subscription with empty subscriber list is removed'
            1 * mockCpsDataService.deleteDataNode('NCMP-Admin', 'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']/filters/filter[@xpath=\'/x/y\']',
                _)
    }
}