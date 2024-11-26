/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

import org.onap.cps.api.model.DataNode
import spock.lang.Specification

class YangDataConverterSpec extends Specification{

    def 'Convert a cm handle data node with private and public properties.'() {
        given: 'a datanode with some additional (dmi, private) and public properties'
            def dataNodeAdditionalProperties = new DataNode(xpath:'/additional-properties[@name="dmiProp1"]',
                    leaves: ['name': 'dmiProp1', 'value': 'dmiValue1'])
            def dataNodePublicProperties = new DataNode(xpath:'/public-properties[@name="pubProp1"]',
                    leaves: ['name': 'pubProp1', 'value': 'pubValue1'])
            def dataNodeCmHandle = new DataNode(leaves:['id':'sample-id'], childDataNodes:[dataNodeAdditionalProperties, dataNodePublicProperties])
        when: 'the dataNode is converted'
            def yangModelCmHandle = YangDataConverter.toYangModelCmHandle(dataNodeCmHandle)
        then: 'the converted object has the correct id'
            assert yangModelCmHandle.id == 'sample-id'
        and: 'the additional (dmi, private) properties are included'
            assert yangModelCmHandle.dmiProperties[0].name == 'dmiProp1'
            assert yangModelCmHandle.dmiProperties[0].value == 'dmiValue1'
        and: 'the public properties are included'
            assert yangModelCmHandle.publicProperties[0].name == 'pubProp1'
            assert yangModelCmHandle.publicProperties[0].value == 'pubValue1'
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
