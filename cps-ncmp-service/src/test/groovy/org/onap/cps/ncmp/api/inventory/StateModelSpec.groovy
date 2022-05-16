/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.api.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.inventory.StateModel.DataStores
import static org.onap.cps.ncmp.api.inventory.StateModel.LockReason
import static org.onap.cps.ncmp.api.inventory.StateModel.Operational
import static org.onap.cps.ncmp.api.inventory.StateModel.Running
import static org.onap.cps.ncmp.utils.TestUtils.getResourceFileContent

class StateModelSpec extends Specification {

    def dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    def aDateAndTime = DateTimeFormatter.ofPattern(dateTimePattern).format(OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 1, ZoneOffset.MIN))
    def objectMapper = new ObjectMapper()

    def "state model specification"() {
        given: "a state model"
            def stateModel = new StateModel(cmhandleState: CmHandleState.ADVISED,
                lockReason: LockReason.builder().reason('lock reason').details("lock misbehaving details").build(),
                lastUpdateTime: aDateAndTime.toString(),
                dataSyncEnabled: Boolean.FALSE,
                dataStores: DataStores.builder().operationalDataStore(
                    Operational.builder()
                        .syncState('NONE_REQUESTED')
                        .lastSyncTime(aDateAndTime.toString()).build()).runningDataStore(
                    Running.builder()
                        .syncState('NONE_REQUESTED')
                        .lastSyncTime(aDateAndTime.toString()).build())
                    .build()
            )
        when: 'represented as JSON'
            def jsonStateModelAsString = objectMapper.writeValueAsString(stateModel)
        then: 'it matches sample JSON'
            def sampleJSON = getResourceFileContent('stateModelSample.json')
            assert sampleJSON == jsonStateModelAsString
    }
}
