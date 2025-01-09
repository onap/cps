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

package org.onap.cps.ncmp.rest.util

import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.rest.model.CmHandleCompositeState
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.LOCKED_MISBEHAVING
import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.MODULE_SYNC_FAILED

class CmHandleStateMapperSpec extends Specification {

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))
    def objectUnderTest = Mappers.getMapper(CmHandleStateMapper)

    def 'Composite State to CmHandleCompositeState'() {
        given: 'a composite state model'
            def compositeState = new CompositeStateBuilder()
                .withCmHandleState(CmHandleState.ADVISED)
                .withLastUpdatedTime(formattedDateAndTime.toString())
                .withLockReason(MODULE_SYNC_FAILED, 'locked details')
                .withOperationalDataStores(DataStoreSyncState.SYNCHRONIZED, formattedDateAndTime).build()
        compositeState.setDataSyncEnabled(false)
        when: 'mapper is called'
            def result = objectUnderTest.toCmHandleCompositeStateExternalLockReason(compositeState)
        then: 'result is of the correct type'
            assert result.class == CmHandleCompositeState.class
        and: 'mapped result should have correct values'
            assert !result.dataSyncEnabled
            assert result.lastUpdateTime == formattedDateAndTime
            assert result.lockReason.reason == MODULE_SYNC_FAILED.name()
            assert result.lockReason.details == 'locked details'
            assert result.cmHandleState == 'ADVISED'
            assert result.dataSyncState.operational.getSyncState() != null
    }

    def 'Handling null state.'() {
        expect: 'converting null returns null'
            CmHandleStateMapper.toDataStores(null) == null
    }

    def 'Internal to External Lock Reason Mapping of #scenario'() {
        given: 'a LOCKED composite state with locked reason of #scenario'
        def compositeState = new CompositeStateBuilder()
                .withCmHandleState(CmHandleState.LOCKED)
                .withLockReason(lockReason, '').build()
        when: 'the composite state is mapped to a CMHandle composite state'
        def result = objectUnderTest.toCmHandleCompositeStateExternalLockReason(compositeState)
        then: 'the composite state contains the expected lock Reason and details'
        result.getLockReason().getReason() == (expectedExternalLockReason as String)
        where:
        scenario             | lockReason         || expectedExternalLockReason
        'MODULE_SYNC_FAILED' | MODULE_SYNC_FAILED || MODULE_SYNC_FAILED
        'null value'         | null               || LOCKED_MISBEHAVING
    }

}
