/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
 * Modifications Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.spi.api.model.DataNode
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static CmSubscriptionPersistenceService.CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE
import static CmSubscriptionPersistenceService.CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH
import static CmSubscriptionPersistenceService.CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_ID
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.spi.api.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.api.FetchDescendantsOption.OMIT_DESCENDANTS

class CmSubscriptionPersistenceServiceSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new CmSubscriptionPersistenceService(jsonObjectMapper, mockCpsQueryService, mockCpsDataService)

    def 'Check ongoing cm subscription #scenario'() {
        given: 'a valid cm subscription query'
            def cpsPathQuery = "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-1']/filters/filter[@xpath='/cps/path']"
        and: 'datanodes optionally returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, OMIT_DESCENDANTS) >> dataNode
        when: 'we check for an ongoing cm subscription'
            def response = objectUnderTest.isOngoingCmSubscription(PASSTHROUGH_RUNNING, 'ch-1', '/cps/path')
        then: 'we get expected response'
            assert response == isOngoingCmSubscription
        where: 'following scenarios are used'
            scenario                  | dataNode                                                                            || isOngoingCmSubscription
            'valid datanodes present' | [new DataNode(xpath: '/cps/path', leaves: ['subscriptionIds': ['sub-1', 'sub-2']])] || true
            'no datanodes present'    | []                                                                                  || false
    }

    def 'Checking uniqueness of incoming subscription ID'() {
        given: 'a cps path with a subscription ID for querying'
            def cpsPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_ID.formatted('some-sub')
        and: 'relevant datanodes are returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions', cpsPathQuery, OMIT_DESCENDANTS) >>
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
            def cpsPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted('ncmp-datastore:passthrough-running', 'ch-1', '/x/y')
        and: 'a dataNode exists for the given cps path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, OMIT_DESCENDANTS) >> [new DataNode(xpath: cpsPathQuery, leaves: ['xpath': '/x/y', 'subscriptionIds': ['sub-1']])]
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmSubscription(PASSTHROUGH_RUNNING, 'ch-1', '/x/y', 'newSubId')
        then: 'data service method to update list of subscribers is called once'
            1 * mockCpsDataService.updateNodeLeaves(
                'NCMP-Admin',
                'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']/filters',
                objectUnderTest.getSubscriptionDetailsAsJson('/x/y', ['sub-1', 'newSubId']), _, ContentType.JSON)
    }

    def 'Add new cm notification subscription for #datastoreType'() {
        given: 'a valid cm subscription path query'
            def cmSubscriptionCpsPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(datastoreName, 'ch-1', '/x/y')
            def cmHandleForSubscriptionPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE.formatted(datastoreName, 'ch-1')
        and: 'a parent node xpath for the cm subscription path above'
            def parentNodeXpath = '/datastores/datastore[@name=\'%s\']/cm-handles'
        and: 'a datanode does not exist for cm subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cmSubscriptionCpsPathQuery,
                OMIT_DESCENDANTS) >> []
        and: 'a datanode does not exist for the given cm handle subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cmHandleForSubscriptionPathQuery, OMIT_DESCENDANTS) >> []
        and: 'subscription is mapped as JSON'
            def subscriptionAsJson = '{"cm-handle":[{"id":"ch-1","filters":' +
                objectUnderTest.getSubscriptionDetailsAsJson('/x/y', ['newSubId']) + '}]}'
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmSubscription(datastoreType, 'ch-1', '/x/y', 'newSubId')
        then: 'data service method to create new subscription for given subscriber is called once with the correct parameters'
            1 * mockCpsDataService.saveData(
                'NCMP-Admin',
                'cm-data-subscriptions',
                parentNodeXpath.formatted(datastoreName),
                subscriptionAsJson, _, ContentType.JSON)
        where:
            scenario                  | datastoreType           || datastoreName
            'passthrough_running'     | PASSTHROUGH_RUNNING     || 'ncmp-datastore:passthrough-running'
            'passthrough_operational' | PASSTHROUGH_OPERATIONAL || 'ncmp-datastore:passthrough-operational'
    }

    def 'Add new cm notification subscription when xpath does not exist for existing subscription cm handle'() {
        given: 'a valid cm subscription path query'
            def cmSubscriptionCpsPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(datastoreName, 'ch-1', '/x/y')
            def cmHandleForSubscriptionPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE.formatted(datastoreName, 'ch-1')
        and: 'a parent node xpath for given cm handle for subscription path above'
            def parentNodeXpath = '/datastores/datastore[@name=\'%s\']/cm-handles/cm-handle[@id=\'%s\']/filters'
        and: 'a datanode does not exist for cm subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cmSubscriptionCpsPathQuery, OMIT_DESCENDANTS) >> []
        and: 'a datanode exists for the given cm handle subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cmHandleForSubscriptionPathQuery, OMIT_DESCENDANTS) >> [new DataNode()]
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmSubscription(datastoreType, 'ch-1', '/x/y', 'newSubId')
        then: 'data service method to create new subscription for given subscriber is called once with the correct parameters'
            1 * mockCpsDataService.saveListElements(
                'NCMP-Admin',
                'cm-data-subscriptions',
                parentNodeXpath.formatted(datastoreName, 'ch-1'),
                objectUnderTest.getSubscriptionDetailsAsJson('/x/y', ['newSubId']), _, ContentType.JSON)
        where:
            scenario                  | datastoreType           || datastoreName
            'passthrough_running'     | PASSTHROUGH_RUNNING     || 'ncmp-datastore:passthrough-running'
            'passthrough_operational' | PASSTHROUGH_OPERATIONAL || 'ncmp-datastore:passthrough-operational'
    }

    def 'Remove subscriber from a list of an ongoing cm notification subscription'() {
        given: 'a subscription exists when queried'
            def cpsPathQuery = CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted('ncmp-datastore:passthrough-running', 'ch-1', '/x/y')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, OMIT_DESCENDANTS) >> [new DataNode(xpath: cpsPathQuery, leaves: ['xpath': '/x/y', 'subscriptionIds': ['sub-1', 'sub-2']])]
        when: 'the subscriber is removed'
            objectUnderTest.removeCmSubscription(PASSTHROUGH_RUNNING, 'ch-1', '/x/y', 'sub-1')
        then: 'the list of subscribers is updated'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']/filters',
                objectUnderTest.getSubscriptionDetailsAsJson('/x/y', ['sub-2']), _, ContentType.JSON)
    }

    def 'Removing last ongoing subscription for datastore and cmhandle and xpath'() {
        given: 'a subscription exists when queried but has only 1 subscriber'
            mockCpsQueryService.queryDataNodes(
                'NCMP-Admin', 'cm-data-subscriptions',
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted('ncmp-datastore:passthrough-running', 'ch-1', '/x/y'),
                OMIT_DESCENDANTS) >> [new DataNode(leaves: ['xpath': '/x/y', 'subscriptionIds': ['sub-1']])]
        and: 'the #scenario'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE.formatted('ncmp-datastore:passthrough-running', 'ch-1'),
                DIRECT_CHILDREN_ONLY) >> [new DataNode(childDataNodes: listOfChildNodes)]
        when: 'that last ongoing subscription is removed'
            objectUnderTest.removeCmSubscription(PASSTHROUGH_RUNNING, 'ch-1', '/x/y', 'sub-1')
        then: 'the subscription with empty subscriber list is removed'
            1 * mockCpsDataService.deleteDataNode('NCMP-Admin', 'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']/filters/filter[@xpath=\'/x/y\']',
                _)
        and: 'method call to delete the cm handle is called the correct number of times'
            numberOfCallsToDeleteCmHandle * mockCpsDataService.deleteDataNode('NCMP-Admin', 'cm-data-subscriptions',
                '/datastores/datastore[@name=\'ncmp-datastore:passthrough-running\']/cm-handles/cm-handle[@id=\'ch-1\']',
                _)
        where:
            scenario                                                          | listOfChildNodes || numberOfCallsToDeleteCmHandle
            'cm handle in same datastore is used for other subscriptions'     | [new DataNode()] || 0
            'cm handle in same datastore is NOT used for other subscriptions' | []               || 1
    }

    def 'Get all nodes for subscription id'() {
        given: 'the query service returns nodes for subscription id'
            def expectedDataNode = new DataNode(xpath: '/some/xpath')
            def queryServiceResponse = [expectedDataNode].asCollection()
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions', '//filter/subscriptionIds[text()=\'some-id\']', OMIT_DESCENDANTS) >> queryServiceResponse
        when: 'retrieving all nodes for subscription id'
            def result = objectUnderTest.getAllNodesForSubscriptionId('some-id')
        then: 'the result returns correct number of datanodes'
            assert result.size() == 1
        and: 'the attribute of the datanode is as expected'
            assert result.iterator().next().xpath == expectedDataNode.xpath
    }

}
