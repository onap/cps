/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration.performance.ncmp

import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.performance.base.NcmpPerfTestBase
import org.onap.cps.spi.api.model.DataNode
import org.onap.cps.utils.ContentType

import static org.onap.cps.spi.api.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CmDataSubscriptionsPerfTest extends NcmpPerfTestBase {

    def datastore1cmHandlePlaceHolder = '{"datastores":{"datastore":[{"name":"ds-1","cm-handles":{"cm-handle":[]}}]}}'
    def xPathForDataStore1CmHandles = '/datastores/datastore[@name="ds-1"]/cm-handles'

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def totalNumberOfEntries = numberOfFiltersPerCmHandle * numberOfCmHandlesPerCmDataSubscription

    def random = new Random()

    def 'Find many subscribers in large dataset.'() {
        when: 'all filters are queried'
            resourceMeter.start()
            def cpsPath = '//filter'
            def result = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'got all filter entries'
            result.size() == totalNumberOfEntries
        then: 'find a random subscriptions by iteration (worst case: whole subscription matches previous entries)'
            def matches = querySubscriptionsByIteration(result, -1)
            resourceMeter.stop()
            matches.size() == numberOfFiltersPerCmHandle * numberOfCmHandlesPerCmDataSubscription
        and: 'query all subscribers within 1 second'
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            recordAndAssertResourceUsage("Query all subscribers", 2.56, durationInSeconds, 300, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Worst case subscription update (200x10 matching entries).'() {
        given: 'all filters are queried'
            def cpsPath = '//filter'
            def result = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
        and: 'there are the expected number of subscribers per subscription'
            assert result.collect {it.leaves.subscribers.size()}.sum() == totalNumberOfEntries * numberOfCmDataSubscribers
        and: 'find all entries for an existing subscriptions'
            def matches = querySubscriptionsByIteration(result, 1)
        when: 'update all subscriptions found'
            resourceMeter.start()
            HashMap<String, List<String>> filterEntriesPerPath = [:]
            matches.each { dataNode, subscribersAsArray ->
                def updatedSubscribers = createLeafList('subscribers', 1 + numberOfCmDataSubscribers, subscriberIdPrefix)
                def filterEntry = '{"xpath":"' + dataNode.leaves.xpath + '", ' + updatedSubscribers + ' }'
                def parentPath = dataNode.xpath.toString().substring(0, dataNode.xpath.toString().indexOf('/filter[@xpath='))
                filterEntriesPerPath.putIfAbsent(parentPath, new ArrayList<String>())
                filterEntriesPerPath.get(parentPath).add(filterEntry)
            }
            HashMap<String, String> jsonPerPath = [:]
            filterEntriesPerPath.each { parentPath, filterEntries ->
                jsonPerPath.put(parentPath, '{"filter": [' + filterEntries.join(',') + ']}')
            }

            // NOTE Below fails as updateDataNodesAndDescendants can't handle JSON lists!
            // cpsDataService.updateDataNodesAndDescendants(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, jsonPerPath, now)

            // So update for each CM-handle instead:
            jsonPerPath.each { parentPath, json ->
                // Around 8.5 seconds for long strings, 4.8 with short strings
                // cpsDataService.updateDataNodeAndDescendants(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, parentPath, json, now)
                // Around 6.5 seconds for long strings, 3.3 seconds with short strings
                cpsDataService.updateNodeLeaves(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, parentPath, json, now, ContentType.JSON)
            }

            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'a subscriber has been added to each filter entry'
            def resultAfter = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
            assert resultAfter.collect {it.leaves.subscribers.size()}.sum() == totalNumberOfEntries * (1 + numberOfCmDataSubscribers)
        and: 'update matching subscription within 15 seconds'
            recordAndAssertResourceUsage("Update matching subscription", 13.86, durationInSeconds, 1000, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Worst case new subscription (200x10 new entries).'() {
        given: 'a new subscription with non-matching data'
            def subscribers = createLeafList('subscribers',1, subscriberIdPrefix)
            def filters = '"filters":' + createJsonArray('filter',numberOfFiltersPerCmHandle,'xpath','other_' + xpathPrefix,subscribers)
            def cmHandles = createJsonArray('cm-handle',numberOfCmHandlesPerCmDataSubscription,'id','other' + cmHandlePrefix, filters)
        when: 'Insert a new subscription'
            resourceMeter.start()
            cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, xPathForDataStore1CmHandles, cmHandles, now)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'insert new subscription with 1 second'
            recordAndAssertResourceUsage("Insert new subscription", 1.28, durationInSeconds, 100, resourceMeter.getTotalMemoryUsageInMB())
    }

    def querySubscriptionsByIteration(Collection<DataNode> allSubscriptionsAsDataNodes, targetSubscriptionSequenceNumber) {
        def matches = [:]
        allSubscriptionsAsDataNodes.each {
            String[] subscribersAsArray = it.leaves.get('subscribers')
            Set<String> subscribersAsSet = new HashSet<>(Arrays.asList(subscribersAsArray))
            def targetSubscriptionId = subscriberIdPrefix + '-' + ( targetSubscriptionSequenceNumber > 0 ? targetSubscriptionSequenceNumber
                    : 1 + random.nextInt(numberOfCmDataSubscribers) )
            if (subscribersAsSet.contains(targetSubscriptionId)) {
                matches.put(it, subscribersAsArray)
            }
        }
        return matches
    }

}
