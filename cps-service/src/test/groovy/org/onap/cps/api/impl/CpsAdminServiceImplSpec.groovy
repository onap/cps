/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl

import org.onap.cps.exceptions.CpsValidationException
import org.onap.cps.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.model.Anchor
import spock.lang.Specification

class CpsAdminServiceImplSpec extends Specification {
    def mockCpsAdminPersistenceService = Mock(CpsAdminPersistenceService)
    def objectUnderTest = new CpsAdminServiceImpl()
    def anchor = new Anchor()

    def setup() {
        objectUnderTest.cpsAdminPersistenceService = mockCpsAdminPersistenceService
    }

    def 'Create an anchor'() {
        given: 'that the persistence service returns the name of the anchor'
            def anchorName = 'some anchor name'
            mockCpsAdminPersistenceService.createAnchor(_) >> anchorName
        expect: 'the same anchor name is returned by CPS Admin service'
            objectUnderTest.createAnchor(anchor) == anchorName
    }

    def 'Create an anchor with some exception in the persistence layer'() {
        given: 'that the persistence service throws some exception'
            def exceptionThrownInPersistenceLayer = new RuntimeException()
            mockCpsAdminPersistenceService.createAnchor(_) >> { throw exceptionThrownInPersistenceLayer }
        when: 'we try to create an anchor'
            objectUnderTest.createAnchor(anchor)
        then: 'the same exception is thrown by the CPS Admin Service'
            def exceptionThrownInServiceLayer = thrown(Exception)
            exceptionThrownInServiceLayer == exceptionThrownInPersistenceLayer
    }

    def 'Retrieve all anchors for an existing dataspace'() {
        given: 'that the dataspace exist and an anchor is associated with the dataspace'
            Collection<Anchor> anchorCollection = Arrays.asList(anchor);
            mockCpsAdminPersistenceService.getAnchors('dummyDataspace') >> { anchorCollection }
        expect: 'we try to retrieve an anchor, a collection of anchor is returned by the service'
            objectUnderTest.getAnchors('dummyDataspace') == anchorCollection
    }

    def 'Retrieve all anchors for a non existing dataspace'() {
        given: 'that the dataspace does not exist, service throws an exception'
            def exceptionThrownInPersistenceLayer = new DataspaceNotFoundException(_ as String)
            mockCpsAdminPersistenceService.getAnchors('dummyDataspace') >>
                    { throw exceptionThrownInPersistenceLayer }
        when: 'we try to retrieve a anchor with a non-existant dataspace'
            objectUnderTest.getAnchors('dummyDataspace')
        then: 'the same exception is thrown by CPS'
            def exceptionThrownInServiceLayer = thrown(Exception)
            exceptionThrownInServiceLayer == exceptionThrownInPersistenceLayer
    }
}
