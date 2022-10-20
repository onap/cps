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

package org.onap.cps.ncmp.rest.controller.handlers

import spock.lang.Specification

class NcmpDatastoreResourceRequestHandlerFactorySpec extends Specification {

    def objectUnderTest = new NcmpDatastoreResourceRequestHandlerFactory(null, null, null)

    def 'Creating ncmp datastore request handlers.'() {
        when: 'a ncmp datastore request handler is created for #datastoreType'
            def result = objectUnderTest.getNcmpDatastoreResourceRequestHandler(datastoreType)
        then: 'the result is of the expected class'
            result.class == expectedClass
        where: 'the following type of datastore is used'
            datastoreType                         || expectedClass
            DatastoreType.OPERATIONAL             || NcmpDatastoreOperationalResourceRequestHandler
            DatastoreType.PASSTHROUGH_OPERATIONAL || NcmpDatastorePassthroughOperationalResourceRequestHandler
            DatastoreType.PASSTHROUGH_RUNNING     || NcmpDatastorePassthroughRunningResourceRequestHandler
    }
}