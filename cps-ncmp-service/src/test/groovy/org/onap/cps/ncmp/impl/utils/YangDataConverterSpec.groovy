/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.utils

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED

import org.onap.cps.api.model.DataNode
import spock.lang.Specification

class YangDataConverterSpec extends Specification{

    def 'Convert a cm handle data node with additional properties, public properties and state.'() {
        given: 'a datanode with some additional properties, public properties and state'
            def dataNodeAdditionalProperties = new DataNode(xpath:'/additional-properties[@name="dmiProp1"]',
                    leaves: ['name': 'dmiProp1', 'value': 'dmiValue1'])
            def dataNodePublicProperties = new DataNode(xpath:'/public-properties[@name="pubProp1"]',
                    leaves: ['name': 'pubProp1', 'value': 'pubValue1'])
            def dataNodeState = new DataNode(xpath:'/parent/state', leaves: ['cm-handle-state': 'ADVISED', 'last-update-time': 'now'])
            def dataNodeCmHandle = new DataNode(leaves:['id':'sample-id', 'alternate-id': 'alt-id', 'module-set-tag': 'my-tag', 'dmi-service-name': 'my-dmi', 'data-producer-identifier': 'my-dpi'], childDataNodes:[dataNodeAdditionalProperties, dataNodePublicProperties, dataNodeState])
        when: 'the dataNode is converted'
            def yangModelCmHandle = YangDataConverter.toYangModelCmHandle(dataNodeCmHandle)
        then: 'the converted object has the fields'
            assert yangModelCmHandle.id == 'sample-id'
            assert yangModelCmHandle.alternateId == 'alt-id'
            assert yangModelCmHandle.dmiServiceName == 'my-dmi'
            assert yangModelCmHandle.moduleSetTag == 'my-tag'
            assert yangModelCmHandle.dataProducerIdentifier == 'my-dpi'
        and: 'the additional properties are included'
            assert yangModelCmHandle.additionalProperties[0].name == 'dmiProp1'
            assert yangModelCmHandle.additionalProperties[0].value == 'dmiValue1'
        and: 'the public properties are included'
            assert yangModelCmHandle.publicProperties[0].name == 'pubProp1'
            assert yangModelCmHandle.publicProperties[0].value == 'pubValue1'
        and: 'the composite state is set'
            assert yangModelCmHandle.compositeState.lastUpdateTime == 'now'
            assert yangModelCmHandle.compositeState.cmHandleState == ADVISED
    }

    def 'Convert multiple cm handle data nodes'(){
        given: 'two data nodes in a collection'
            def dataNodes = [new DataNode(xpath:'/dmi-registry/cm-handles[@id=\'some-cm-handle\']', leaves: ['id':'some-cm-handle']),
                             new DataNode(xpath:'/dmi-registry/cm-handles[@id=\'another-cm-handle\']', leaves: ['id':'another-cm-handle'])]
        when: 'the data nodes are converted'
            def yangModelCmHandles = YangDataConverter.toYangModelCmHandles(dataNodes)
        then: 'verify both have returned and CmHandleIds are correct'
            assert yangModelCmHandles.size() == 2
            assert yangModelCmHandles.id.containsAll(['some-cm-handle', 'another-cm-handle'])
    }
}
