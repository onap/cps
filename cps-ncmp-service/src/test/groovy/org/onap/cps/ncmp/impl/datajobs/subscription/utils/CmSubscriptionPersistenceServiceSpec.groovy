/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.utils

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.ACCEPTED

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.model.DataNode
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static CmDataJobSubscriptionPersistenceService.*
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class CmSubscriptionPersistenceServiceSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new CmDataJobSubscriptionPersistenceService(jsonObjectMapper, mockCpsQueryService, mockCpsDataService)

    def 'Check cm data job subscription details has at least one subscriber #scenario'() {
        given: 'a valid cm data job subscription query'
            def cpsPathQuery = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted('/myDataNodeSelector')
        and: 'datanodes optionally returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cpsPathQuery, OMIT_DESCENDANTS) >> dataNode
        when: 'we check if subscription details already has at least one subscriber'
            def result = objectUnderTest.hasAtLeastOneSubscription('/myDataNodeSelector')
        then: 'we get expected result'
            assert result == hasAtLeastOneSubscription
        where: 'following scenarios are used'
            scenario                  | dataNode                                              || hasAtLeastOneSubscription
            'valid datanodes present' | [new DataNode(leaves: ['dataJobId': ['dataJobId1']])] || true
            'no datanodes present'    | []                                                    || false
    }

    def 'Checking uniqueness of incoming subscription ID'() {
        given: 'a cps path with a data job subscription ID for querying'
            def cpsPathQuery = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted('mySubId')
        and: 'collection of data nodes are returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cpsPathQuery, OMIT_DESCENDANTS) >> dataNodes
        when: 'a data job subscription id is tested for uniqueness'
            def result = objectUnderTest.isNewSubscriptionId('mySubId')
        then: 'result is as expected'
            assert result == isValidDataJobSubscriptionId
        where: 'following scenarios are used'
            scenario               | dataNodes        || isValidDataJobSubscriptionId
            'datanodes present'    | [new DataNode()] || false
            'no datanodes present' | []               || true
    }

    def 'Get all inactive data node selectors for subscription id'() {
        given: 'the query service returns nodes for subscription id'
            def expectedDataNode = new DataNode(leaves: ['datajobId': ['id1'], 'dataNodeSelector': '/dataNodeSelector', 'status': 'UNKNOWN'])
            def queryServiceResponse = [expectedDataNode].asCollection()
            def cmDataJobSubscriptionIdCpsPath = CPS_PATH_TEMPLATE_FOR_INACTIVE_SUBSCRIPTIONS.formatted('id1')
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cmDataJobSubscriptionIdCpsPath, OMIT_DESCENDANTS) >> queryServiceResponse
        when: 'retrieving all nodes for data job subscription id'
            def result = objectUnderTest.getInactiveDataNodeSelectors('id1')
        then: 'the result returns correct number of datanodes'
            assert result.size() == 1
        and: 'the attribute of the data nodes is as expected'
            assert result.iterator().next() == expectedDataNode.leaves.dataNodeSelector
    }

    def 'Add subscription for a data node selector that have no subscriptions yet.'() {
        given: 'a valid cm data job subscription path query'
            def dataNodeSelector = '/myDataNodeSelector'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector)
        and: 'a data node does not exist for cm data job subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> []
        and: 'data job subscription details is mapped as JSON'
            def subscriptionIds = ['newSubId']
            def subscriptionAsJson = objectUnderTest.createSubscriptionDetailsAsJson(dataNodeSelector, subscriptionIds, 'UNKNOWN')
        when: 'the method to add cm notification subscription is called'
            objectUnderTest.add('newSubId', dataNodeSelector)
        then: 'data service method to create new subscription for given subscriber is called once with the correct parameters'
            1 * mockCpsDataService.saveData('NCMP-Admin', 'cm-data-job-subscriptions', PARENT_NODE_XPATH, subscriptionAsJson, _, ContentType.JSON)
    }

    def 'Add subscription for a data node selector that already have subscription(s).'() {
        given: 'a valid cm subscription path query'
            def dataNodeSelector = '/myDataNodeSelector'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector)
        and: 'a dataNode exists for the given cps path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> [new DataNode(leaves: ['dataJobId': ['existingId'], 'dataNodeSelector': dataNodeSelector, 'status': 'ACCEPTED'])]
        and: 'updated cm data job subscription details as json'
            def newListOfSubscriptionIds = ['existingId', 'newSubId']
            def subscriptionDetailsAsJson = objectUnderTest.createSubscriptionDetailsAsJson(dataNodeSelector, newListOfSubscriptionIds, 'ACCEPTED')
        when: 'the method to add cm notification subscription is called'
            objectUnderTest.add('newSubId', dataNodeSelector)
        then: 'data service method to update list of subscribers is called once'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', PARENT_NODE_XPATH, subscriptionDetailsAsJson, _, ContentType.JSON)
    }

    def 'Get data node selectors by subscription id'() {
        given: 'a subscription id and a corresponding CPS query path'
            def subscriptionId = 'mySubId'
            def cpsPathQuery = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(subscriptionId)

        and: 'the query service returns a collection of DataNodes with dataNodeSelectors'
            def expectedDataNode1 = new DataNode(leaves: ['dataNodeSelector': '/node1'])
            def expectedDataNode2 = new DataNode(leaves: ['dataNodeSelector': '/node2'])
            def queryServiceResponse = [expectedDataNode1, expectedDataNode2]
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cpsPathQuery, OMIT_DESCENDANTS) >> queryServiceResponse

        when: 'getDataNodeSelectorsBySubscriptionId is called'
            def result = objectUnderTest.getDataNodeSelectorsBySubscriptionId(subscriptionId)

        then: 'the returned list contains the correct data node selectors'
            assert result.size() == 2
            assert result.contains('/node1')
            assert result.contains('/node2')
    }

    def 'Delete subscription removes last subscriber and sets status as UNKNOWN'() {
        given: 'a dataNode with only one subscription'
            def dataNodeSelector = '/myDataNodeSelector'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector)
            def dataNode = new DataNode(leaves: ['dataJobId': ['id1'], 'status': 'ACCEPTED'])
            mockCpsQueryService.queryDataNodes('NCMP-Admin','cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> [dataNode]

        and: 'subscription IDs for the data node'
            objectUnderTest.getSubscriptionIds(dataNodeSelector) >> ['id1']

        when: 'delete method is called'
            objectUnderTest.delete('id1', dataNodeSelector)

        then: 'data service is called to update status to UNKNOWN'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', PARENT_NODE_XPATH, { json ->
                json.contains('"status":"UNKNOWN"') && json.contains('"dataJobId":[]')
            }, _, ContentType.JSON)
    }

    def 'Delete subscription removes one of multiple subscribers'() {
        given: 'a dataNode with multiple subscriptions'
            def dataNodeSelector = '/myDataNodeSelector'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector)
            def subscriptionIdToRemove = 'id1'
            def remainingSubscriptionId = 'id2'
            def dataNode = new DataNode(leaves: ['dataJobId': [subscriptionIdToRemove, remainingSubscriptionId], 'status': 'ACCEPTED'])
            mockCpsQueryService.queryDataNodes('NCMP-Admin','cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> [dataNode]

        and: 'subscription IDs for the data node'
            objectUnderTest.getSubscriptionIds(dataNodeSelector) >> [subscriptionIdToRemove, remainingSubscriptionId]

        when: 'delete method is called'
            objectUnderTest.delete(subscriptionIdToRemove, dataNodeSelector)

        then: 'data service is called to update leaves with remaining subscription'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', PARENT_NODE_XPATH, { json ->
                json.contains('"status":"ACCEPTED"') &&
                        json.contains('"dataJobId":["id2"]')
            }, _, ContentType.JSON)
    }

    def 'UpdateStatus sets the new status while keeping existing subscriptions'() {
        given: 'a data node with subscriptions'
            def dataNodeSelector = '/myDataNodeSelector'
            def subscriptionIds = ['id1', 'id2']

            objectUnderTest = Spy(objectUnderTest)
            objectUnderTest.getSubscriptionIds(dataNodeSelector) >> subscriptionIds

        when: 'updateStatus method is called'
            objectUnderTest.updateStatus(dataNodeSelector, 'ACCEPTED')

        then: 'data service is called to update leaves with the new status'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', PARENT_NODE_XPATH, { json ->
                json.contains('"status":"ACCEPTED"') &&
                        json.contains('"dataJobId":["id1","id2"]')
            }, _, ContentType.JSON)
    }

    def 'Update subscription status'() {
        given: 'a data node selector'
            def myDataNodeSelector = "/myDataNodeSelector"
        and: 'a status'
            def status = ACCEPTED
        and: 'the query service returns data node'
            def subscriptionIds = ['someId']
            mockCpsQueryService.queryDataNodes(_,_,_,_) >> [new DataNode(leaves: ['dataJobId': subscriptionIds, 'dataNodeSelector': myDataNodeSelector, 'status': 'UNKNOWN'])]
        and: 'updated cm data job subscription details as json'
            def subscriptionDetailsAsJson = objectUnderTest.createSubscriptionDetailsAsJson(myDataNodeSelector, subscriptionIds, status.name())
        when: 'the method to update subscription status is called'
            objectUnderTest.updateCmSubscriptionStatus(myDataNodeSelector, status)
        then: 'data service method to update list of subscribers is called once'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', PARENT_NODE_XPATH, subscriptionDetailsAsJson, _, _)

    }

}
