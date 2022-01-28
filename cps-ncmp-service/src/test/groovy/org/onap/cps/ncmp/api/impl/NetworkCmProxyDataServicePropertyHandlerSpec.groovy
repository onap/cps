/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl

import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.models.CmHandle
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class NetworkCmProxyDataServicePropertyHandlerSpec extends Specification {

	def mockCpsDataService = Mock(CpsDataService)

	def objectUnderTest = new NetworkCmProxyDataServicePropertyHandler(mockCpsDataService)
	def dataspaceName = 'NCMP-Admin'
	def anchorName = 'ncmp-dmi-registry'
	def cmHandleId = 'myHandle1'
	def parentXpath = '/dmi-registry'
	def cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}']"

	def 'Update DataNodes based on updated cmHandles properties(DMI and Public) when #scenario'() {
		given: 'a dataNode based on cmHandleId'
			def dataNode = new DataNode()
			dataNode.xpath = cmHandleXpath
			dataNode.leaves = existingProperties
			mockCpsDataService.getDataNode(dataspaceName, anchorName, cmHandleXpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
		and: 'updated cmHandles request with public and DMI properties'
			def cmHandleUpdateRequest = [new CmHandle(cmHandleID: cmHandleId, dmiProperties: updatedDmiProperties, publicProperties: updatedPublicProperties)]
		when: 'updateDataNodeLeaves is called using correct parameters'
			def updatedDataNodes = objectUnderTest.updateDataNodeLeaves(dataspaceName, anchorName, parentXpath, cmHandleUpdateRequest)
		then: 'we get back Collection of updated dataNodes'
			assert updatedDataNodes.leaves == [expectedPropertiesAfterUpdate]
		where:
			scenario                                    | existingProperties                       | updatedDmiProperties  | updatedPublicProperties     || expectedPropertiesAfterUpdate
			'property removed'                          | ['prop': 'value', 'pub-prop': 'pub-val'] | ['prop': null]        | ['pub-prop': null]          || [:]
			'property updated'                          | ['prop': 'value', 'pub-prop': 'pub-val'] | ['prop': 'newValue']  | ['pub-prop': 'newPubVal']   || ['prop': 'newValue', 'pub-prop': 'newPubVal']
			'property added'                            | ['prop': 'value']                        | ['new-prop': 'value'] | ['new-pub-prop': 'pub-val'] || ['prop': 'value', 'new-prop': 'value', 'new-pub-prop': 'pub-val']
			'property ignored(value is null)'           | ['prop': 'value', 'pub-prop': 'pub-val'] | ['propx': null]       | ['pub-propx': null]         || ['prop': 'value', 'pub-prop': 'pub-val']
			'no existing property and we try to add'    | [:]                                      | ['prop4': 'value4']   | ['pub-prop4': 'val4']       || ['prop4': 'value4', 'pub-prop4': 'val4']
			'no existing property and we try to remove' | [:]                                      | ['prop4': null]       | ['pub-prop4': null]         || [:]
	}

	def 'Exception is thrown when we are trying to update cmHandle when it is not found'() {
		given: 'update cmHandles request with public and DMI properties'
			def cmHandleUpdateRequest = [new CmHandle(cmHandleID: cmHandleId, publicProperties: [:], dmiProperties: [:])]
		and: 'when we try to find a unknown dataNode'
			mockCpsDataService.getDataNode(dataspaceName, anchorName, cmHandleXpath,
				FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >>
				{ throw new DataNodeNotFoundException(dataspaceName, anchorName, cmHandleXpath) }
		when: 'updateDataNodeLeaves is called using correct parameters'
			objectUnderTest.updateDataNodeLeaves(dataspaceName, anchorName, parentXpath, cmHandleUpdateRequest)
		then: 'DataValidationException is thrown'
			def exceptionThrown = thrown(DataValidationException.class)
			assert exceptionThrown.getMessage().contains('DataNode not found')

	}
}
