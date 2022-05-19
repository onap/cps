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

package org.onap.cps.ncmp.rest.mapper

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.ncmp.rest.model.RestOutputCmHandleState
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.inventory.CompositeState.DataStores
import static org.onap.cps.ncmp.api.inventory.CompositeState.LockReason
import static org.onap.cps.ncmp.api.inventory.CompositeState.Operational

class RestOutputCmHandleStateMapperTest extends Specification {

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))
    def objectUnderTest = Mappers.getMapper(RestOutputCmHandleStateMapper)

    def 'Composite State to Rest Output CmHandleState'() {
        given: 'a composite state model'
            def compositeState = new CompositeState(cmHandleState: CmHandleState.ADVISED,
                lockReason: LockReason.builder().lockReasonCategory(LockReasonCategory.LOCKED_MISBEHAVING).details('locked other details').build(),
                lastUpdateTime: formattedDateAndTime.toString(),
                dataSyncEnabled: false,
                dataStores: dataStores())
        when: 'mapper is called'
            def result = objectUnderTest.toRestOutputCmHandleState(compositeState)
        then: 'result is of the correct type'
            assert result.class == RestOutputCmHandleState.class
        and: 'mapped result should have correct values'
            assert !result.dataSyncEnabled
            assert result.lastUpdateTime == formattedDateAndTime
            assert result.lockReason.reason == 'LOCKED_MISBEHAVING'
            assert result.lockReason.details == 'locked other details'
            assert result.cmHandleState == CmHandleState.ADVISED.name()
            assert result.dataSyncState.operational != null
            assert result.dataSyncState.running != null
    }

    def dataStores() {

        return DataStores.builder()
            .operationalDataStore(Operational.builder()
                .syncState('NONE_REQUESTED')
                .lastSyncTime(formattedDateAndTime.toString()).build())
            .runningDataStore(CompositeState.Running.builder()
                .syncState('SYNCHRONIZED')
                .lastSyncTime(formattedDateAndTime.toString()).build())
            .build()
    }
}
