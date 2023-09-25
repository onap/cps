/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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
import org.onap.cps.spi.model.DataNode

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CmDataSubscribersPerfTest extends NcmpPerfTestBase {

    def datastore1cmHandlePlaceHolder = '{"datastores":{"datastore":[{"name":"ds-1","cm-handles":{"cm-handle":[]}}]}}'
    def xPathForDataStore1CmHandles = '/datastores/datastore[@name="ds-1"]/cm-handles'

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def totalNumberOfEntries = numberOfFiltersPerCmHandle * numberOfCmHandlesPerCmDataSubscription

    def random = new Random()

    def 'Find many subscribers in large dataset.'() {
        when: 'all filters are queried'
            stopWatch.start()
            def cpsPath = '//filter'
            def result = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'got all filter entries'
            result.size() == totalNumberOfEntries
        then: 'find a random subscriptions by iteration (worst case: whole subscription matches previous entries)'
            def matches = querySubscriptionsByIteration(result, -1)
            stopWatch.stop()
            matches.size() == numberOfFiltersPerCmHandle * numberOfCmHandlesPerCmDataSubscription
        and: 'query all subscribers within 1 second'
            def durationInMillis = stopWatch.getTotalTimeMillis()
            recordAndAssertPerformance("Query all subscribers", 1_000, durationInMillis)
    }

    def 'Worst case new subscription (200x10 new entries).'() {
        given: 'a new subscription with non-matching data'
            def subscribers = createLeafList('subscribers',1, subscriberIdPrefix)
            def filters = '"filters":' + createJsonArray('filter',numberOfFiltersPerCmHandle,'xpath','other_' + xpathPrefix,subscribers)
            def cmHandles = createJsonArray('cm-handle',numberOfCmHandlesPerCmDataSubscription,'id','other' + cmHandlePrefix, filters)
        when: 'Insert a new subscription'
            stopWatch.start()
            cpsDataService.saveData(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, xPathForDataStore1CmHandles, cmHandles, now)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'insert new subscription with 1 second'
            recordAndAssertPerformance("Insert new subscription", 1_000, durationInMillis)
    }

    def 'Worst case subscription update (200x10 matching entries).'() {
        given: 'all filters are queried'
            def cpsPath = '//filter'
            def result = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
        and: 'find all entries for an existing subscriptions'
            def matches = querySubscriptionsByIteration(result, 1)
        when: 'Update all subscriptions found'
            stopWatch.start()
            /* the production code version of this should manipulate the original subscribersAsArray of course
               but for the (performance) poc creating another array with one extra element suffices
             */
            def jsonPerPath = [:]
            matches.each { xpath, subscribersAsArray ->
                def updatedSubscribers = createLeafList('subscribers', 1 + numberOfCmDataSubscribers, subscriberIdPrefix)
                def filterEntry = '{"filter": {"xpath":"' + xpath + '", ' + updatedSubscribers + ' } }'
                def parentPath = xpath.toString().substring(0, xpath.toString().indexOf('/filter[@xpath='))
                jsonPerPath.put(parentPath, filterEntry)
            }
            cpsDataService.updateDataNodesAndDescendants(NCMP_PERFORMANCE_TEST_DATASPACE, CM_DATA_SUBSCRIPTIONS_ANCHOR, jsonPerPath, now)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'Update matching subscription within 8 seconds'
            //TODO Toine check with Daniel if this can be optimized quickly without really changing production code
            // ie is there a better way of doing these 2,000 updates
            recordAndAssertPerformance("Update matching subscription", 8_000, durationInMillis)
    }

    def querySubscriptionsByIteration(Collection<DataNode> allSubscriptionsAsDataNodes, targetSubscriptionSequenceNumber) {
        def matches = [:]
        allSubscriptionsAsDataNodes.each {
            String[] subscribersAsArray = it.leaves.get('subscribers')
            Set<String> subscribersAsSet = new HashSet<>(Arrays.asList(subscribersAsArray))
            def targetSubscriptionId = subscriberIdPrefix + '-' + ( targetSubscriptionSequenceNumber > 0 ? targetSubscriptionSequenceNumber
                                                                                                     : 1 + random.nextInt(numberOfCmDataSubscribers) )
            if (subscribersAsSet.contains(targetSubscriptionId)) {
                matches.put(it.xpath, subscribersAsArray)
            }
        }
        return matches
    }

}
