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

package org.onap.cps.ncmp.rest.controller.handlers

import org.spockframework.spring.SpringBean
import spock.lang.Specification

class NcmpDatastoreRequestHandlerFactorySpec extends Specification {

    @SpringBean
    NcmpCachedResourceRequestHandler mockNcmpCachedResourceRequestHandler = new NcmpCachedResourceRequestHandler(null)

    @SpringBean
    NcmpPassthroughResourceRequestHandler mockNcmpPassthroughResourceRequestHandler = new NcmpPassthroughResourceRequestHandler()

    def objectUnderTest = new NcmpDatastoreResourceRequestHandlerFactory(mockNcmpCachedResourceRequestHandler, mockNcmpPassthroughResourceRequestHandler)

    def 'Creating ncmp datastore request handlers.'() {
        when: 'a ncmp datastore request handler is created for #datastoreType'
            def result = objectUnderTest.getNcmpResourceRequestHandler(datastoreType)
        then: 'the result is of the expected class'
            result.class == expectedClass
        where: 'the following type of datastore is used'
            datastoreType                         || expectedClass
            DatastoreType.OPERATIONAL             || NcmpCachedResourceRequestHandler
            DatastoreType.PASSTHROUGH_OPERATIONAL || NcmpPassthroughResourceRequestHandler
            DatastoreType.PASSTHROUGH_RUNNING     || NcmpPassthroughResourceRequestHandler
    }
}
