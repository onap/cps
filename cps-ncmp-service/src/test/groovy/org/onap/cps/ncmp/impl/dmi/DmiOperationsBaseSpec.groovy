/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.dmi

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.spockframework.spring.SpringBean
import spock.lang.Shared
import spock.lang.Specification

abstract class DmiOperationsBaseSpec extends Specification {

    @Shared
    def yangModelCmHandleProperty = new YangModelCmHandle.Property('prop1', 'val1')

    @SpringBean
    DmiRestClient mockDmiRestClient = Mock()

    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock()

    @SpringBean
    ObjectMapper spyObjectMapper = Spy()

    def yangModelCmHandle = new YangModelCmHandle()
    def static dmiServiceName = 'myServiceName'
    def static cmHandleId = 'some-cm-handle'
    def static alternateId = 'alt-id-' + cmHandleId
    def static resourceIdentifier = 'parent/child'

    def mockYangModelCmHandleRetrieval(dmiProperties) {
        populateYangModelCmHandle(dmiProperties, '')
        mockInventoryPersistence.getYangModelCmHandle(cmHandleId) >> yangModelCmHandle
    }

    def mockYangModelCmHandleRetrieval(dmiProperties, moduleSetTag) {
        populateYangModelCmHandle(dmiProperties, moduleSetTag)
        mockInventoryPersistence.getYangModelCmHandle(cmHandleId) >> yangModelCmHandle
    }

    def mockYangModelCmHandleCollectionRetrieval(dmiProperties) {
        populateYangModelCmHandle(dmiProperties, '')
        mockInventoryPersistence.getYangModelCmHandlesFromCmHandleReferences(_) >> [yangModelCmHandle]
    }

    def populateYangModelCmHandle(dmiProperties, moduleSetTag) {
        yangModelCmHandle.dmiDataServiceName = dmiServiceName
        yangModelCmHandle.dmiServiceName = dmiServiceName
        yangModelCmHandle.dmiProperties = dmiProperties
        yangModelCmHandle.id = cmHandleId
        yangModelCmHandle.alternateId = alternateId
        yangModelCmHandle.compositeState = new CompositeState()
        yangModelCmHandle.compositeState.cmHandleState = CmHandleState.READY
        yangModelCmHandle.moduleSetTag = moduleSetTag
    }
}
