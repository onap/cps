/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cmavc

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.model.Anchor
import org.onap.cps.cpspath.parser.CpsPathUtil
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent
import org.onap.cps.ncmp.events.avc1_0_0.Data
import org.onap.cps.ncmp.events.avc1_0_0.DatastoreChanges
import org.onap.cps.ncmp.events.avc1_0_0.Edit
import org.onap.cps.ncmp.events.avc1_0_0.IetfYangPatchYangPatch
import org.onap.cps.ncmp.events.avc1_0_0.PushChangeUpdate
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.YangParser
import spock.lang.Specification

class CmAvcEventServiceSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)
    def mockYangParser = Mock(YangParser)

    def static NO_TIMESTAMP = null

    def objectUnderTest = new CmAvcEventService(
        mockCpsDataService,
        mockCpsAnchorService,
        mockJsonObjectMapper,
        mockYangParser
    )

    def cmHandleId = 'test-cmhandle-id'
    def sampleJson = '{"some-data": "test-data"}'

    def setup() {
        mockJsonObjectMapper.asJsonString(_) >> sampleJson
        mockJsonObjectMapper.convertJsonString(_, String) >> sampleJson
    }

    def 'process CREATE operation'() {
        given: 'An edit with CREATE operation'
            def edit = Mock(Edit) {
                getOperation() >> 'create'
                getValue() >> new Object()
            }
            def testAvcEvent = buildMockedAvcEventWithEdits([edit])
        when: 'The AVC event is processed'
            objectUnderTest.processCmAvcEvent(cmHandleId, testAvcEvent)
        then: 'data is saved via cps service'
            1 * mockCpsDataService.saveData(_, cmHandleId, sampleJson, NO_TIMESTAMP)
    }

    def 'Process UPDATE operation'() {
        given: 'An edit with UPDATE operation and a valid target path'
            def targetPath = '/test/path'
            def edit = Mock(Edit) {
                getOperation() >> 'update'
                getTarget() >> targetPath
                getValue() >> new Object()
            }
            def anchor = Mock(Anchor)
            mockCpsAnchorService.getAnchor(_, cmHandleId) >> anchor
            mockYangParser.getCpsPathFromRestConfStylePath(anchor, targetPath) >> '/parsed/cps/path'
            def testAvcEventForUpdate = buildMockedAvcEventWithEdits([edit])
        when: 'The AVC event is processed'
            objectUnderTest.processCmAvcEvent(cmHandleId, testAvcEventForUpdate)
        then: 'Data node and descendants are updated via CPS service'
            1 * mockCpsDataService.updateDataNodeAndDescendants(_, cmHandleId, _, sampleJson, NO_TIMESTAMP, _)
    }

    def 'Process PATCH operation'() {
        given: 'An edit with PATCH operation and a valid target path'
            def targetPath = '/test/path'
            def edit = Mock(Edit) {
                getOperation() >> 'patch'
                getTarget() >> targetPath
                getValue() >> new Object()
            }
            def anchor = Mock(Anchor)
            mockCpsAnchorService.getAnchor(_, cmHandleId) >> anchor
            mockYangParser.getCpsPathFromRestConfStylePath(anchor, targetPath) >> '/parsed/cps/path'
            def testAvcEventForPatch = buildMockedAvcEventWithEdits([edit])
        when: 'The AVC event is processed'
            objectUnderTest.processCmAvcEvent(cmHandleId, testAvcEventForPatch)
        then: 'Node leaves are updated via CPS service'
            1 * mockCpsDataService.updateNodeLeaves(_, cmHandleId, _, sampleJson, NO_TIMESTAMP, _)
    }

    def 'Process DELETE operation with target'() {
        given: 'An edit with DELETE operation and a specific target path'
            def targetPath = '/test/path'
            def edit = Mock(Edit) {
                getOperation() >> 'delete'
                getTarget() >> targetPath
            }
            def anchor = Mock(Anchor)
            mockCpsAnchorService.getAnchor(_, cmHandleId) >> anchor
            mockYangParser.getCpsPathFromRestConfStylePath(anchor, targetPath) >> '/parsed/cps/path'
            def testAvcEventForDelete = buildMockedAvcEventWithEdits([edit])
        when: 'The AVC event is processed'
            objectUnderTest.processCmAvcEvent(cmHandleId, testAvcEventForDelete)
        then: 'Data node is deleted at the given path'
            1 * mockCpsDataService.deleteDataNode(_, cmHandleId, '/parsed/cps/path', NO_TIMESTAMP)
    }

    def 'Process DELETE operation with no target (delete all)'() {
        given: 'An edit with DELETE operation and no target'
            def edit = Mock(Edit) {
                getOperation() >> 'delete'
                getTarget() >> target
            }
            def testAvcEventForDelete = buildMockedAvcEventWithEdits([edit])
        when: 'The AVC event is processed'
            objectUnderTest.processCmAvcEvent(cmHandleId, testAvcEventForDelete)
        then: 'All data nodes for the cmHandleId are deleted'
            1 * mockCpsDataService.deleteDataNodes(_, cmHandleId, NO_TIMESTAMP)
        where: 'following targets are used'
            target << [null, '']
    }

    def 'Resolve parent xpath correctly: #scenario'() {
        expect: 'Parent xpath is resolved as expected'
            assert objectUnderTest.resolveParentNodeXpath(inputXpath) == expectedXpath
        where: 'following scenarios are used'
            scenario                        | inputXpath   || expectedXpath
            'when parentXpath is empty'     | ''           || CpsPathUtil.ROOT_NODE_XPATH
            'when parentXpath is not empty' | '/test/path' || '/test/path'
    }

    def buildMockedAvcEventWithEdits(edits) {
        return Mock(AvcEvent) {
            getData() >> Mock(Data) {
                getPushChangeUpdate() >> Mock(PushChangeUpdate) {
                    getDatastoreChanges() >> Mock(DatastoreChanges) {
                        getIetfYangPatchYangPatch() >> Mock(IetfYangPatchYangPatch) {
                            getEdit() >> edits
                        }
                    }
                }
            }
        }
    }
}
