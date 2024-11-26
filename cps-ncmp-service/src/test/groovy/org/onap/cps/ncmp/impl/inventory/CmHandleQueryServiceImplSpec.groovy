/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.inventory

import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.impl.utils.CpsValidator
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.spi.api.model.DataNode
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT
import static org.onap.cps.spi.api.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.api.FetchDescendantsOption.OMIT_DESCENDANTS

class CmHandleQueryServiceImplSpec extends Specification {

    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsDataService = Mock(CpsDataService)

    def trustLevelPerDmiPlugin = [:]

    def trustLevelPerCmHandle = [ 'PNFDemo': TrustLevel.COMPLETE, 'PNFDemo2': TrustLevel.NONE, 'PNFDemo4': TrustLevel.NONE ]

    def mockCpsValidator = Mock(CpsValidator)

    def objectUnderTest = new CmHandleQueryServiceImpl(mockCpsDataService, mockCpsQueryService, trustLevelPerDmiPlugin, trustLevelPerCmHandle, mockCpsValidator)

    @Shared
    def static sampleDataNodes = [new DataNode()]

    def dataNodeWithPrivateField = '//additional-properties[@name=\"Contact3\" and @value=\"newemailforstore3@bookstore.com\"]/ancestor::cm-handles'

    def static pnfDemo = createDataNode('PNFDemo')
    def static pnfDemo2 = createDataNode('PNFDemo2')
    def static pnfDemo3 = createDataNode('PNFDemo3')
    def static pnfDemo4 = createDataNode('PNFDemo4')
    def static pnfDemo5 = createDataNode('PNFDemo5')

    def 'Query CmHandles with public properties query pair.'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service.'
            mockResponses()
        when: 'a query on cmhandle public properties is performed with a public property pair'
            def result = objectUnderTest.queryCmHandlePublicProperties(publicPropertyPairs, outputAlternateId)
        then: 'the correct cm handle data objects are returned'
            result.containsAll(expectedCmHandleReferences)
            result.size() == expectedCmHandleReferences.size()
        where: 'the following data is used'
            scenario                         | publicPropertyPairs                                                                      | outputAlternateId || expectedCmHandleReferences
            'single property matches'        | [Contact: 'newemailforstore@bookstore.com']                                              | false             || ['PNFDemo', 'PNFDemo2', 'PNFDemo4']
            'public property does not match' | [wont_match: 'wont_match']                                                               | false             || []
            '2 properties, only one match'   | [Contact: 'newemailforstore@bookstore.com', Contact2: 'newemailforstore2@bookstore.com'] | true              || ['alt-PNFDemo4']
            '2 properties, no matches'       | [Contact: 'newemailforstore@bookstore.com', Contact2: '']                                | false             || []
    }

    def 'Query cm handles on trust level'() {
        given: 'query properties for trust level COMPLETE'
            def trustLevelPropertyQueryPairs = ['trustLevel' : TrustLevel.COMPLETE.toString()]
        and: 'the dmi cache has been initialised and "knows" about my-dmi-plugin-identifier'
            trustLevelPerDmiPlugin.put('my-dmi-plugin-identifier', TrustLevel.COMPLETE)
        and: 'the DataNodes queried for a given cpsPath are returned from the persistence service'
            mockResponses()
        when: 'the query is run'
            def result = objectUnderTest.queryCmHandlesByTrustLevel(trustLevelPropertyQueryPairs, outputAlternateId)
        then: 'the result contain trusted cmHandle reference'
            assert result.size() == 1
            assert result[0] == expectedCmHandleReference
        where: 'the following data is used'
            senario               | outputAlternateId | expectedCmHandleReference
            'output cmHandleId'   |  false            | 'PNFDemo'
            'output AlternateId'  |  true             | 'alt-PNFDemo'
    }

    def 'Query CmHandles using empty public properties query pair.'() {
        when: 'a query on CmHandle public properties is executed using an empty map'
            def result = objectUnderTest.queryCmHandlePublicProperties([:], false)
        then: 'no cm handles are returned'
            result.size() == 0
    }

    def 'Query CmHandles using empty private properties query pair.'() {
        when: 'a query on CmHandle private properties is executed using an empty map'
            def result = objectUnderTest.queryCmHandleAdditionalProperties([:], false)
        then: 'no cm handles are returned'
            result.size() == 0
    }

    def 'Query CmHandles by a private field\'s value.'() {
        given: 'a data node exists with a certain additional-property'
            mockCpsQueryService.queryDataNodes(_, _, dataNodeWithPrivateField, _) >> [pnfDemo5]
        when: 'a query on CmHandle private properties is executed using a map'
            def result = objectUnderTest.queryCmHandleAdditionalProperties(['Contact3': 'newemailforstore3@bookstore.com'], false)
        then: 'one cm handle is returned'
            result.size() == 1
    }

    def 'Get CmHandles by it\'s state.'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.ADVISED
        and: 'the persistence service returns a list of data nodes'
            mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                '//state[@cm-handle-state="ADVISED"]/ancestor::cm-handles', INCLUDE_ALL_DESCENDANTS) >> sampleDataNodes
        when: 'cm handles are fetched by state'
            def result = objectUnderTest.queryCmHandlesByState(cmHandleState)
        then: 'the returned result matches the result from the persistence service'
            assert result == sampleDataNodes
    }

    def 'Check the state of a cmHandle when #scenario.'() {
        given: 'a cm handle state to compare'
            def cmHandleState = state
        and: 'the persistence service returns a list of data nodes'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    NCMP_DMI_REGISTRY_PARENT + '/cm-handles[@id=\'some-cm-handle\']/state',
                    OMIT_DESCENDANTS) >> [new DataNode(leaves: ['cm-handle-state': 'READY'])]
        when: 'cm handles are compared by state'
            def result = objectUnderTest.cmHandleHasState('some-cm-handle', cmHandleState)
        then: 'the returned result matches the expected result from the persistence service'
            result == expectedResult
        where:
            scenario                           | state                 || expectedResult
            'the provided state matches'       | CmHandleState.READY   || true
            'the provided state does not match'| CmHandleState.DELETED || false
    }

    def 'Get Cm Handles state by Cm-Handle Id'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            mockCpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    NCMP_DMI_REGISTRY_PARENT + '/cm-handles[@id=\'some-cm-handle\']/state',
                    OMIT_DESCENDANTS) >> [new DataNode(leaves: ['cm-handle-state': 'READY'])]
        when: 'cm handles are fetched by state and id'
            def result = objectUnderTest.getCmHandleState('some-cm-handle')
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == new DataNode(leaves: ['cm-handle-state': 'READY'])
    }

    def 'Retrieve Cm Handles By Operational Sync State : UNSYNCHRONIZED'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                '//state/datastores/operational[@sync-state="'+'UNSYNCHRONIZED'+'"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'cm handles are fetched by the UNSYNCHRONIZED operational sync state'
            def result = objectUnderTest.queryCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Retrieve cm handle by cps path '() {
        given: 'a cm handle state to query based on the cps path'
            def cmHandleDataNode = new DataNode(xpath: "/dmi-registry/cm-handles[@id='ch-1']", leaves: ['id': 'ch-1'])
            def cpsPath = "//state[@cm-handle-state='LOCKED']"
        and: 'cps data service returns a valid data node for cm handle ancestor'
            mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cpsPath + '/ancestor::cm-handles', INCLUDE_ALL_DESCENDANTS)
                >> Arrays.asList(cmHandleDataNode)
        when: 'get cm handles by cps path is invoked'
            def result = objectUnderTest.queryCmHandleAncestorsByCpsPath(cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result.contains(cmHandleDataNode)
    }

    def 'Retrieve cm handle by cps path querying cm handle directly'() {
        given: 'a cm handle to query based on the cps path'
            def cmHandleDataNode = new DataNode(xpath: "/dmi-registry/cm-handles[@id='ch-2']", leaves: ['id': 'ch-2'])
            def cpsPath = "//cm-handles[@alternate-id='1']"
        and: 'cps data service returns a valid data node'
            mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                    cpsPath, INCLUDE_ALL_DESCENDANTS)
                    >> Arrays.asList(cmHandleDataNode)
        when: 'get cm handles by cps path is invoked'
            def result = objectUnderTest.queryCmHandleAncestorsByCpsPath(cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result.contains(cmHandleDataNode)
    }

    def 'Get all cm handles by dmi plugin identifier and alternate id output option where #scenario'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service.'
            mockResponses()
        when: 'cm Handles are fetched for a given dmi plugin identifier and alternate id output option'
            def result = objectUnderTest.getCmHandleReferencesByDmiPluginIdentifier('my-dmi-plugin-identifier', outputAlternateId)
        then: 'result is the correct size'
            assert result.size() == 3
        and: 'result contains the correct cm handles'
            assert result.containsAll(expectedResult)
        where:
            scenario                        | outputAlternateId || expectedResult
            'output is for alternate ids'   | true              || ['alt-PNFDemo', 'alt-PNFDemo2', 'alt-PNFDemo4']
            'output is for cm handle ids'   | false             || ['PNFDemo', 'PNFDemo2', 'PNFDemo4']
    }

    def 'Get all alternateIds by dmi plugin identifier'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service.'
            mockResponses()
        when: 'cm Handles are fetched for a given dmi plugin identifier'
            def result = objectUnderTest.getCmHandleReferencesMapByDmiPluginIdentifier('my-dmi-plugin-identifier').values()
        then: 'result is the correct size'
            assert result.size() == 3
        and: 'result contains the correct alternate Ids'
            assert result.containsAll('alt-PNFDemo', 'alt-PNFDemo2', 'alt-PNFDemo4')
    }

    void mockResponses() {
        mockCpsQueryService.queryDataNodes(_, _, '//public-properties[@name=\"Contact\" and @value=\"newemailforstore@bookstore.com\"]/ancestor::cm-handles', _) >> [pnfDemo, pnfDemo2, pnfDemo4]
        mockCpsQueryService.queryDataNodes(_, _, '//public-properties[@name=\"wont_match\" and @value=\"wont_match\"]/ancestor::cm-handles', _) >> []
        mockCpsQueryService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"newemailforstore2@bookstore.com\"]/ancestor::cm-handles', _) >> [pnfDemo4]
        mockCpsQueryService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"\"]/ancestor::cm-handles', _) >> []
        mockCpsQueryService.queryDataNodes(_, _, '//state[@cm-handle-state=\"READY\"]/ancestor::cm-handles', _) >> [pnfDemo, pnfDemo3]
        mockCpsQueryService.queryDataNodes(_, _, '//state[@cm-handle-state=\"LOCKED\"]/ancestor::cm-handles', _) >> [pnfDemo2, pnfDemo4]
        mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, '/dmi-registry/cm-handles[@dmi-service-name=\'my-dmi-plugin-identifier\']', OMIT_DESCENDANTS) >> [pnfDemo, pnfDemo2]
        mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, '/dmi-registry/cm-handles[@dmi-data-service-name=\'my-dmi-plugin-identifier\']', OMIT_DESCENDANTS) >> [pnfDemo, pnfDemo4]
        mockCpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, '/dmi-registry/cm-handles[@dmi-model-service-name=\'my-dmi-plugin-identifier\']', OMIT_DESCENDANTS) >> [pnfDemo2, pnfDemo4]
    }

    def static createDataNode(dataNodeId) {
        return new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'' + dataNodeId + '\']', leaves: ['id':dataNodeId, 'alternate-id':'alt-' + dataNodeId])
    }
}
