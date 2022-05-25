/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada
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
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.YangUtils
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static CompositeState.*
import static org.onap.cps.ncmp.utils.TestUtils.getResourceFileContent
import static org.springframework.util.StringUtils.trimAllWhitespace

class CompositeStateBuilderSpec extends Specification {

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def "Composite State Specification"() {
        when: 'using composite state builder '
            def compositeState = new CompositeStateBuilder().withCmHandleState(CmHandleState.ADVISED)
                    .withLockReason("lock-reason","").withOperationalDataStores("UNSYNCHRONIZED",
                    formattedDateAndTime.toString()).withLastUpdatedTime(formattedDateAndTime).build();
        then: 'it matches expected cm handle state and data store sync state'
            assert compositeState.getCmhandleState() == CmHandleState.ADVISED
            assert compositeState.dataStores.operationalDataStore.syncState == 'UNSYNCHRONIZED'
    }

    def "Build composite state from DataNode "() {
        given: "a Data Node "
            def dataNode = new DataNode(leaves: ['cm-handle-state': 'ADVISED'])
        when: 'build from data node function is invoked'
            def compositeState = new CompositeStateBuilder().fromDataNode(dataNode).build()
        then: 'it matches expected state model as JSON'
            assert compositeState.cmhandleState == CmHandleState.ADVISED
    }

}
