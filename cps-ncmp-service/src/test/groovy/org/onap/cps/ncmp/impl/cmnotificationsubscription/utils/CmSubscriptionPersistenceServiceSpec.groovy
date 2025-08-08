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

import static CmDataJobSubscriptionPersistenceService.CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH
import static CmDataJobSubscriptionPersistenceService.CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE
import static CmDataJobSubscriptionPersistenceService.CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.model.DataNode
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmSubscriptionPersistenceServiceSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new CmDataJobSubscriptionPersistenceService(jsonObjectMapper, mockCpsQueryService, mockCpsDataService)

    def 'Check cm data job subscription details has at least one subscriber #scenario'() {
        given: 'a valid cm data job subscription query'
            def cpsPathQuery = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('altId1', 'dataType1')
        and: 'datanodes optionally returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cpsPathQuery, OMIT_DESCENDANTS) >> dataNode
        when: 'we check if subscription details already has at least one subscriber'
            def result = objectUnderTest.hasAtLeastOneSubscription('dataType1', 'altId1')
        then: 'we get expected result'
            assert result == hasAtLeastOneSubscription
        where: 'following scenarios are used'
            scenario                  | dataNode                                             || hasAtLeastOneSubscription
            'valid datanodes present' | [new DataNode(leaves: ['dataJobId': ['dataJobId1']])]|| true
            'no datanodes present'    | []                                                   || false
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

    def 'Get all nodes for subscription id'() {
        given: 'the query service returns nodes for subscription id'
            def expectedDataNode = new DataNode(leaves: ['datajobId': ['id1'], 'dataTypeId': 'dataType1', 'alternateId': 'altId1'])
            def queryServiceResponse = [expectedDataNode].asCollection()
            def cmDataJobSubscriptionIdCpsPath = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted('mySubId')
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', cmDataJobSubscriptionIdCpsPath, OMIT_DESCENDANTS) >> queryServiceResponse
        when: 'retrieving all nodes for data job subscription id'
            def result = objectUnderTest.getAffectedDataNodes('mySubId')
        then: 'the result returns correct number of datanodes'
            assert result.size() == 1
        and: 'the attribute of the data nodes is as expected'
            assert result.iterator().next().leaves.alternateId == expectedDataNode.leaves.alternateId
            assert result.iterator().next().leaves.dataTypeId == expectedDataNode.leaves.dataTypeId
    }

    def 'Add subscription for a data type and and fdn that have no subscriptions yet.'() {
        given: 'a valid cm data job subscription path query'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('altId1', 'dataType1')
        and: 'a data node does not exist for cm data job subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> []
        and: 'a datanode does not exist for the given cm data job subscription path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions', query, OMIT_DESCENDANTS) >> []
        and: 'data job subscription details is mapped as JSON'
            def subscriptionIds = ['newSubId']
            def subscriptionAsJson = objectUnderTest.getSubscriptionDetailsAsJson(subscriptionIds, 'dataType1', 'altId1')
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addSubscription('dataType1', 'altId1', 'newSubId')
        then: 'data service method to create new subscription for given subscriber is called once with the correct parameters'
            1 * mockCpsDataService.saveData('NCMP-Admin', 'cm-data-job-subscriptions', subscriptionAsJson, _, ContentType.JSON)
    }

    def 'Add subscription for a data type and fdn that already have subscription(s).'() {
        given: 'a valid cm subscription path query'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('altId1', 'dataType1')
        and: 'a dataNode exists for the given cps path query'
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> [new DataNode(leaves: ['dataJobId': ['existingId'], 'dataTypeId': 'dataType1', 'alternateId': 'altId1'])]
        and: 'updated cm data job subscription details as json'
            def newListOfSubscriptionIds = ['existingId', 'newSubId']
            def subscriptionDetailsAsJson = objectUnderTest.getSubscriptionDetailsAsJson(newListOfSubscriptionIds, 'dataType1', 'altId1')
        when: 'the method to add/update cm notification subscription is called'
            objectUnderTest.addSubscription('dataType1', 'altId1', 'newSubId')
        then: 'data service method to update list of subscribers is called once'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH, subscriptionDetailsAsJson, _, ContentType.JSON)
    }

    def 'Remove subscription (other subscriptions remain for same data type and target).'() {
        given: 'a subscription exists when queried'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('altId1', 'dataType1')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS)
                >> [new DataNode(leaves: ['dataJobId': ['existingId','subIdToRemove'], 'dataTypeId': 'dataType1', 'alternateId': 'altId1'])]
        and: 'updated cm data job subscription details as json'
            def subscriptionDetailsAsJson = objectUnderTest.getSubscriptionDetailsAsJson(['existingId'], 'dataType1', 'altId1')
        when: 'the subscriber is removed'
            objectUnderTest.removeSubscription('dataType1', 'altId1','subIdToRemove')
        then: 'the list of subscribers is updated'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'cm-data-job-subscriptions', CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH, subscriptionDetailsAsJson, _, ContentType.JSON)
    }

    def 'Remove last subscription (no subscriptions remain for same data type and target).'() {
        given: 'a subscription exists when queried but has only 1 subscriber'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('last-alt-id', 'last-data-type')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS)
                >> [new DataNode(leaves: ['dataJobId': ['subIdToRemove'], 'dataTypeId': 'last-data-type', 'alternateId': 'last-alt-id'])]
        and: 'a cps path with alternate id and data type for deleting a node'
            def cpsPath = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('last-alt-id', 'last-data-type')
        when: 'that last ongoing subscription is removed'
            objectUnderTest.removeSubscription('last-data-type', 'last-alt-id','subIdToRemove')
        then: 'the data job subscription with empty subscribers list is removed'
            1 * mockCpsDataService.deleteDataNode('NCMP-Admin', 'cm-data-job-subscriptions', cpsPath, _)
    }

    def 'Attempt to remove non existing subscription (id).'() {
        given: 'a subscription exists when queried with other subscriber'
            def query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted('some-alt-id', 'some-data-type')
            mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-job-subscriptions', query, OMIT_DESCENDANTS) >> [new DataNode(leaves: ['dataJobId': ['otherDataJobId']])]
        when: 'the remove subscription method is with a non existing id'
            objectUnderTest.removeSubscription('some-data-type', 'some-alt-id','nonExistingSubId')
        then: 'no calls to cps data service is made'
            0 * mockCpsDataService.deleteDataNode(*_)
        and: 'removal of non existent subscription id silently ignored with no exception thrown'
            noExceptionThrown()
    }

}
