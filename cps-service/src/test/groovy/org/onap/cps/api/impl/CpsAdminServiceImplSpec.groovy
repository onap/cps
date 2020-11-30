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
import org.onap.cps.spi.CpsAdminService
import org.onap.cps.spi.model.Anchor
import spock.lang.Specification

class CpsAdminServiceImplSpec extends Specification {
    def mockCpsAdminService = Mock(CpsAdminService)
    def objectUnderTest = new CpsAdminServiceImpl()

    def setup() {
        objectUnderTest.cpsAdminService = mockCpsAdminService
    }

    def 'Create an anchor with a non-existant dataspace'() {
        given: 'that the dataspace does not exist service throws an exception'
            Anchor anchor = new Anchor()
            anchor.setDataspaceName('dummyDataspace')
            mockCpsAdminService.createAnchor(anchor) >> { throw new CpsValidationException(_ as String, _ as String) }
        when: 'we try to create a anchor with a non-existant dataspace'
            objectUnderTest.createAnchor(anchor)
        then: 'the same exception is thrown by CPS'
            thrown(CpsValidationException)
    }

    def 'Create an anchor with invalid dataspace, namespace and revision'() {
        given: 'that the dataspace, namespace and revison combination does not exist service throws an exception'
            Anchor anchor = new Anchor()
            anchor.setDataspaceName('dummyDataspace')
            anchor.setNamespace('dummyNamespace')
            anchor.setRevision('dummyRevision')
            mockCpsAdminService.createAnchor(anchor) >> { throw new CpsValidationException(_ as String, _ as String) }
        when: 'we try to create a anchor with a non-existant dataspace, namespace and revison combination'
            objectUnderTest.createAnchor(anchor)
        then: 'the same exception is thrown by CPS'
            thrown(CpsValidationException)
    }

    def 'Create a duplicate anchor'() {
        given: 'that the anchor already exist service throws an exception'
            Anchor anchor = new Anchor()
            anchor.setDataspaceName('dummyDataspace')
            anchor.setNamespace('dummyNamespace')
            anchor.setRevision('dummyRevision')
            anchor.setRevision('dummyAnchorName')
            mockCpsAdminService.createAnchor(anchor) >> { throw new CpsValidationException(_ as String, _ as String) }
        when: 'we try to create a duplicate anchor'
            objectUnderTest.createAnchor(anchor)
        then: 'the same exception is thrown by CPS'
            thrown(CpsValidationException)
    }

    def 'Create an anchor with supplied anchor name, dataspace, namespace and revision'() {
        given: 'that the anchor does not pre-exist service creates an anchor'
            Anchor anchor = new Anchor()
            anchor.setDataspaceName('dummyDataspace')
            anchor.setNamespace('dummyNamespace')
            anchor.setRevision('dummyRevision')
            anchor.setRevision('dummyAnchorName')
            mockCpsAdminService.createAnchor(anchor) >> 'dummyAnchorName'
        expect: 'anchor name is returned by service'
            objectUnderTest.createAnchor(anchor) == 'dummyAnchorName'
    }

}
