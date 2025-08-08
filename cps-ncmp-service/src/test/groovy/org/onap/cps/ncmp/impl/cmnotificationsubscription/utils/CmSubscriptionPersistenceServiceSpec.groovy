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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.model.DataNode
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static CmSubscriptionPersistenceService.CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH
import static CmSubscriptionPersistenceService.CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE
import static CmSubscriptionPersistenceService.CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_DATA_JOB_ID
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class CmSubscriptionPersistenceServiceSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new CmSubscriptionPersistenceService(jsonObjectMapper, mockCpsQueryService, mockCpsDataService)

    def 'Check ongoing cm data job subscription #scenario'() {
        given: 'a valid cm data job subscription query'
            def cpsPathQuery = CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('someAlternateId', 'someDataType')
        and: 'datanodes optionally returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                cpsPathQuery, OMIT_DESCENDANTS) >> dataNode
        when: 'we check for an ongoing cm data job subscription'
            def response = objectUnderTest.isOngoingCmDataJobSubscription('someDataType', 'someAlternateId')
        then: 'we get expected response'
            assert response == isOngoingCmDataJobSubscription
        where: 'following scenarios are used'
            scenario                  | dataNode                                             || isOngoingCmDataJobSubscription
            'valid datanodes present' | [new DataNode(leaves: ['dataJobId': ['dataJobId1']])]|| true
            'no datanodes present'    | []                                                   || false
    }

    def 'Checking uniqueness of incoming subscription ID'() {
        given: 'a cps path with a data job subscription ID for querying'
            def cpsPathQuery = CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted('someDataJobId')
        and: 'relevant data nodes are returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                cpsPathQuery, OMIT_DESCENDANTS) >> dataNodes
        when: 'a data job subscription ID is tested for uniqueness'
            def result = objectUnderTest.isUniqueDataJobSubscriptionId('someDataJobId')
        then: 'result is as expected'
            assert result == isValidDataJobSubscriptionId
        where: 'following scenarios are used'
            scenario               | dataNodes        || isValidDataJobSubscriptionId
            'datanodes present'    | [new DataNode()] || false
            'no datanodes present' | []               || true
    }

    def 'Get all nodes for subscription id'() {
        given: 'the query service returns nodes for subscription id'
            def expectedDataNode = new DataNode(leaves: ['datajobId': ['id1'], 'dataTypeId': 'some-data-type', 'alternateId': 'some-alt-id'])
            def queryServiceResponse = [expectedDataNode].asCollection()
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted('some-id'), OMIT_DESCENDANTS) >> queryServiceResponse
        when: 'retrieving all nodes for data job subscription id'
            def result = objectUnderTest.getAllNodesForDataJobSubscriptionId('some-id')
        then: 'the result returns correct number of datanodes'
            assert result.size() == 1
        and: 'the attribute of the data nodes is as expected'
            assert result.iterator().next().leaves.alternateId == expectedDataNode.leaves.alternateId
            assert result.iterator().next().leaves.dataTypeId == expectedDataNode.leaves.dataTypeId
    }

    def 'Add new subscriber to an ongoing cm notification data job subscription: data type and alternate id exists'() {
        given: 'a valid cm subscription path query'
            def cmSubscriptionCpsPathQuery = CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('some-alt-id', 'some-data-type')
        and: 'a dataNode exists for the given cps path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                cmSubscriptionCpsPathQuery, OMIT_DESCENDANTS) >> [new DataNode(leaves: ['dataJobId': ['existingId'], 'dataTypeId': 'some-data-type', 'alternateId': 'some-alt-id'])]
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmDataJobSubscription('some-data-type', 'some-alt-id', 'newSubId')
        then: 'data service method to update list of subscribers is called once'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH,
                objectUnderTest.getSubscriptionDetailsAsJson(['existingId', 'newSubId'], 'some-data-type', 'some-alt-id'), _, ContentType.JSON)
    }

    def 'Add new cm notification data job subscription for data type and alternate Id'() {
        given: 'a valid cm data job subscription path query'
            def cmSubscriptionCpsPathQuery = CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('some-alt-id', 'some-data-type')
        and: 'a data node does not exist for cm data job subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                cmSubscriptionCpsPathQuery,
                OMIT_DESCENDANTS) >> []
        and: 'a datanode does not exist for the given cm data job subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cmSubscriptionCpsPathQuery, OMIT_DESCENDANTS) >> []
        and: 'data job subscription details is mapped as JSON'
            def dataJobSubscriptionIds = ["newSubId"]
            def subscriptionAsJson = objectUnderTest.getSubscriptionDetailsAsJson(dataJobSubscriptionIds, 'some-data-type', 'some-alt-id')
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addCmDataJobSubscription('some-data-type', 'some-alt-id', 'newSubId')
        then: 'data service method to create new subscription for given subscriber is called once with the correct parameters'
            1 * mockCpsDataService.saveData('NCMP-Admin', 'cm-data-job-subscriptions', subscriptionAsJson, _, ContentType.JSON)
    }

    def 'Remove subscriber from a list of an ongoing cm notification subscription'() {
        given: 'a subscription exists when queried'
            def cmSubscriptionCpsPathQuery = CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('some-alt-id', 'some-data-type')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions',
                cmSubscriptionCpsPathQuery, OMIT_DESCENDANTS) >> [new DataNode(leaves: ['dataJobId': ['existingId','subIdToRemove'], 'dataTypeId': 'some-data-type', 'alternateId': 'some-alt-id'])]
        when: 'the subscriber is removed'
            objectUnderTest.removeCmDataJobSubscription('some-data-type', 'some-alt-id','subIdToRemove')
        then: 'the list of subscribers is updated'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH,
                objectUnderTest.getSubscriptionDetailsAsJson(['existingId'], 'some-data-type', 'some-alt-id'), _, ContentType.JSON)
    }

    def 'Removing last ongoing subscription for datastore and cmhandle and xpath'() {
        given: 'a subscription exists when queried but has only 1 subscriber'
            def cmSubscriptionCpsPathQuery = CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('last-alt-id', 'last-data-type')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cmSubscriptionCpsPathQuery, OMIT_DESCENDANTS)
                >> [new DataNode(leaves: ['dataJobId': ['subIdToRemove'], 'dataTypeId': 'last-data-type', 'alternateId': 'last-alt-id'])]
        when: 'that last ongoing subscription is removed'
            objectUnderTest.removeCmDataJobSubscription('last-data-type', 'last-alt-id','subIdToRemove')
        then: 'the data job subscription with empty subscribers list is removed'
            1 * mockCpsDataService.deleteDataNode('NCMP-Admin', 'cm-data-job-subscriptions',
                CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('last-alt-id', 'last-data-type'), _)
    }

}
