/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration.functional

import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.DataspaceInUseException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException

class CpsAdminServiceIntegrationSpec extends CpsIntegrationSpecBase {

    CpsAdminService objectUnderTest

    CpsAnchorService cpsAnchorService

    def setup() { objectUnderTest = cpsAdminService }

    def 'Dataspace CRUD operations.'() {
        when: 'a dataspace is created'
            objectUnderTest.createDataspace('newDataspace')
        then: 'the dataspace can be read'
            assert objectUnderTest.getDataspace('newDataspace').name == 'newDataspace'
        and: 'it can be deleted'
            objectUnderTest.deleteDataspace('newDataspace')
        then: 'the dataspace no longer exists i.e. an exception is thrown if an attempt is made to retrieve it'
            def thrown = null
            try {
                objectUnderTest.getDataspace('newDataspace')
            } catch(Exception exception) {
                thrown = exception
            }
           assert thrown instanceof DataspaceNotFoundException
    }

    def 'Delete dataspace with error; #scenario.'() {
        setup: 'add some anchors if needed'
            numberOfAnchors.times {
                cpsAnchorService.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'anchor' + it)
            }
        when: 'attempt to delete dataspace'
            objectUnderTest.deleteDataspace(dataspaceName)
        then: 'the correct exception is thrown with the relevant details'
            def thrownException = thrown(expectedException)
            thrownException.details.contains(expectedMessageDetails)
        cleanup:
            numberOfAnchors.times {
                cpsAnchorService.deleteAnchor(GENERAL_TEST_DATASPACE, 'anchor' + it)
            }
        where: 'the following data is used'
            scenario                        | dataspaceName          | numberOfAnchors || expectedException          | expectedMessageDetails
            'dataspace name does not exist' | 'unknown'              | 0               || DataspaceNotFoundException | 'unknown does not exist'
            'dataspace contains schemasets' | GENERAL_TEST_DATASPACE | 0               || DataspaceInUseException    | 'contains 1 schemaset(s)'
            //'dataspace contains anchors'    | GENERAL_TEST_DATASPACE | 2               || DataspaceInUseException    | 'contains 2 anchor(s)'
    }

    def 'Retrieve all dataspaces (depends on total test suite).'() {
        given: 'two addtional dataspaces are created'
            objectUnderTest.createDataspace('dataspace1')
            objectUnderTest.createDataspace('dataspace2')
        when: 'all datespaces are retreived'
            def result = objectUnderTest.getAllDataspaces()
        then: 'there are at least 3 dataspaces (2 new ones plus the general test dataspace)'
            result.size() >= 3
            assert result.name.containsAll([GENERAL_TEST_DATASPACE, 'dataspace1', 'dataspace2'])
    }

    def 'Duplicate dataspaces.'() {
        when: 'attempting to create a dataspace with the same name as an existing one'
            objectUnderTest.createDataspace(GENERAL_TEST_DATASPACE)
        then: 'an exception is thrown indicating the dataspace already exists'
            thrown(AlreadyDefinedException)
    }

}
