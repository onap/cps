/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.data.models

import org.onap.cps.ncmp.api.data.exceptions.InvalidOperationException
import spock.lang.Specification

class OperationTypeSpec extends Specification {

    def 'Converting string to enum.'() {
        expect: 'converting string to enum results in the correct enum value'
            OperationType.fromOperationName(operationName) == expectedEnum
        where: 'the following datastore names are used'
            operationName || expectedEnum
            'read'        || OperationType.READ
            'create'      || OperationType.CREATE
            'update'      || OperationType.UPDATE
            'patch'       || OperationType.PATCH
            'delete'      || OperationType.DELETE
    }

    def 'Converting unknown name string to enum.'() {
        when: 'attempt converting unknown datastore name'
            OperationType.fromOperationName('unknown')
        then: 'an invalid operation exception is thrown'
            def thrown = thrown(InvalidOperationException)
            assert thrown.message.contains('unknown is an invalid operation')
    }

}
