/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.models

import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.CompositeState
import spock.lang.Specification

class NcmpServiceCmHandleSpec extends Specification {


    def 'NCMP Service CmHandle check for deep copy operation'() {
        given: 'ncmp service cm handle'
            def originalNcmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'cmhandleid',
                dmiProperties: ['property1': 'value1', 'property2': 'value2'],
                publicProperties: ['pubproperty1': 'value1', 'pubproperty2': 'value2'],
                compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED, dataSyncEnabled: Boolean.FALSE))
        when: 'we create a deep copy'
            def deepCopiedNcmpServiceCmHandle = new NcmpServiceCmHandle(originalNcmpServiceCmHandle)
        and: 'we change the original ncmp service cmhandle'
            originalNcmpServiceCmHandle.dmiProperties = ['newProperty1': 'newValue1']
            originalNcmpServiceCmHandle.publicProperties = ['newPublicProperty1': 'newPubValue1']
            originalNcmpServiceCmHandle.compositeState = new CompositeState(cmHandleState: CmHandleState.DELETED, dataSyncEnabled: Boolean.TRUE)
        then: 'no change in the copied dmi and public properties of ncmp service cmhandle'
            deepCopiedNcmpServiceCmHandle.dmiProperties == ['property1': 'value1', 'property2': 'value2']
            deepCopiedNcmpServiceCmHandle.publicProperties == ['pubproperty1': 'value1', 'pubproperty2': 'value2']
        and: 'no change in the composite state'
            deepCopiedNcmpServiceCmHandle.compositeState.cmHandleState == CmHandleState.ADVISED
            deepCopiedNcmpServiceCmHandle.compositeState.dataSyncEnabled == Boolean.FALSE
    }

}
