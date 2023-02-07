/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils

import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class YangDataConverterSpec extends Specification{

    def 'Convert a cm handle data node with private properties.'() {
        given: 'a datanode with some additional (dmi, private) properties'
            def dataNodeAdditionalProperties = new DataNode(xpath:'/additional-properties[@name="dmiProp1"]',
                leaves: ['name': 'dmiProp1', 'value': 'dmiValue1'])
            def dataNode = new DataNode(childDataNodes:[dataNodeAdditionalProperties])
        when: 'the dataNode is converted'
            def yangModelCmHandle = YangDataConverter.convertCmHandleToYangModel(dataNode,'sample-id')
        then: 'the converted object has the correct id'
            assert yangModelCmHandle.id == 'sample-id'
        and: 'the additional (dmi, private) properties are included'
            assert yangModelCmHandle.dmiProperties[0].name == 'dmiProp1'
            assert yangModelCmHandle.dmiProperties[0].value == 'dmiValue1'
    }

    def 'Convert multiple cm handle data nodes'(){
        given: 'two data nodes in a collection one with private properties'
            def dataNodeAdditionalProperties = new DataNode(xpath:'/additional-properties[@name="dmiProp1"]',
                    leaves: ['name': 'dmiProp1', 'value': 'dmiValue1'])
            def dataNodes = [new DataNode(xpath:'/dmi-registry/cm-handles[@id=\'some-cm-handle\']'),
                             new DataNode(xpath:'/dmi-registry/cm-handles[@id=\'another-cm-handle\']', childDataNodes:[dataNodeAdditionalProperties])]
        when: 'the data nodes are converted'
            def yangModelCmHandles = YangDataConverter.convertDataNodesToYangModelCmHandles(dataNodes)
        then: 'verify both have returned and cmhandleIds are correct'
            assert yangModelCmHandles.size() == 2
            assert yangModelCmHandles.id.containsAll(['some-cm-handle', 'another-cm-handle'])
    }

}
