/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.models

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.inventory.models.CompositeState.DataStores
import static org.onap.cps.ncmp.api.inventory.models.CompositeState.Operational
import static org.onap.cps.ncmp.utils.TestUtils.getResourceFileContent
import static org.springframework.util.StringUtils.trimAllWhitespace

class CompositeStateSpec extends Specification {

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))
    def objectMapper = new ObjectMapper()

    def "Composite State Specification"() {
        given: "a Composite State"
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LockReasonCategory.MODULE_SYNC_FAILED).details("lock details").build(),
                lastUpdateTime: formattedDateAndTime.toString(),
                dataSyncEnabled: false,
                dataStores: dataStores())
        when: 'it is represented as JSON'
            def resultJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(compositeState)
        then: 'it matches expected state model as JSON'
            def expectedJson = getResourceFileContent('expectedStateModel.json')
            assert trimAllWhitespace(expectedJson) == trimAllWhitespace(resultJson)
    }

    def dataStores() {
        DataStores.builder().operationalDataStore(Operational.builder()
            .dataStoreSyncState(DataStoreSyncState.NONE_REQUESTED)
            .lastSyncTime(formattedDateAndTime.toString()).build())
            .build()
    }
}
