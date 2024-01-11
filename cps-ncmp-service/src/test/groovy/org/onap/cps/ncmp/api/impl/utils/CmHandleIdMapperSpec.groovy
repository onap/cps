/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.utils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.slf4j.LoggerFactory
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import spock.lang.Specification

class CmHandleIdMapperSpec extends Specification {

    def alternateIdPerCmHandle = new HashMap<String, String>()
    def cmHandlePerAlternateId = new HashMap<String, String>()
    def mockCpsCmHandlerQueryService = Mock(NetworkCmProxyCmHandleQueryService)

    def objectUnderTest = new CmHandleIdMapper(alternateIdPerCmHandle, cmHandlePerAlternateId, mockCpsCmHandlerQueryService)

    def logger = Spy(ListAppender<ILoggingEvent>)

    def setup() {
        ((Logger) LoggerFactory.getLogger(CmHandleIdMapper.class)).addAppender(logger)
        logger.start()
        mockCpsCmHandlerQueryService.getAllCmHandles() >> []
        assert objectUnderTest.addMapping('my cmhandle id', 'my alternate id')
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmHandleIdMapper.class)).detachAndStopAllAppenders()
    }

    def 'Checking entries in the cache.'() {
        expect: 'the alternate id can be converted to cmhandle id'
            assert objectUnderTest.alternateIdToCmHandleId('my alternate id') == 'my cmhandle id'
        and: 'the cmhandle id can be converted to alternate id'
            assert objectUnderTest.cmHandleIdToAlternateId('my cmhandle id') == 'my alternate id'
    }

    def 'Attempt adding #scenario alternate id.'() {
        expect: 'cmhandle id - alternate id mapping fails'
            assert objectUnderTest.addMapping('ch-1', alternateId) == false
        and: 'alternate id looked up by cmhandle id unsuccessfully'
            assert getObjectUnderTest().cmHandleIdToAlternateId('ch-1') == null
        where: 'alternate id has an invalid value'
            scenario | alternateId
            'empty'  | ''
            'blank'  | '  '
            'null'   | null
    }

    def 'Remove an entry from the cache.'() {
        when: 'removing an entry'
            objectUnderTest.removeMapping('my cmhandle id')
        then: 'converting alternate id returns null'
            assert objectUnderTest.alternateIdToCmHandleId('my alternate id') == null
        and: 'converting cmhandle id returns null'
            assert objectUnderTest.cmHandleIdToAlternateId('my cmhandle id') == null
    }

    def 'Cannot update existing alternate id.'() {
        given: 'attempt to update an existing alternate id'
            objectUnderTest.addMapping('my cmhandle id', 'other id')
        expect: 'still returns the original alternate id'
            assert objectUnderTest.cmHandleIdToAlternateId('my cmhandle id') == 'my alternate id'
        and: 'converting other alternate id returns null'
            assert objectUnderTest.alternateIdToCmHandleId('other id') == null
        and: 'a warning is logged with the original alternate id'
            def lastLoggingEvent = logger.list[1]
            assert lastLoggingEvent.level == Level.WARN
            assert lastLoggingEvent.formattedMessage.contains('my alternate id')
    }

    def 'Update existing alternate id with the same value.'() {
        expect: 'update an existing alternate id with the same value returns false (no update)'
            assert objectUnderTest.addMapping('my cmhandle id', 'my alternate id') == false
        and: 'conversion still returns the original alternate id'
            assert objectUnderTest.cmHandleIdToAlternateId('my cmhandle id') == 'my alternate id'
    }

    def 'Initializing cache #scenario.'() {
        when: 'the cache is (re-)initialized'
            objectUnderTest.cacheIsInitialized = false
            objectUnderTest.initializeCache()
        then: 'the alternate id can be converted to cmhandle id'
            assert objectUnderTest.alternateIdToCmHandleId('alt-1') == convertedCmHandleId
        and: 'the cm handle id can be converted to alternate id'
            assert objectUnderTest.cmHandleIdToAlternateId('ch-1') == convertedAlternatId
        and: 'the query service is called to get the initial data'
            1 * mockCpsCmHandlerQueryService.getAllCmHandles() >> persistedCmHandles
        where: 'the initial data has a cm handle #scenario'
            scenario                  | persistedCmHandles                                                  || convertedAlternatId | convertedCmHandleId
            'with alternate id'       | [new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: 'alt-1')] || 'alt-1'             | 'ch-1'
            'without alternate id'    | [new NcmpServiceCmHandle(cmHandleId: 'ch-1')]                       || null                | null
            'with blank alternate id' | [new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: ' ')]     || null                | null
    }
}