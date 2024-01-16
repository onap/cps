/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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

package org.onap.cps.utils

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
import spock.lang.Specification

class YangUtilsSpec extends Specification {

    def 'Get key attribute statement without key attributes'() {
        given: 'a path argument without key attributes'
            def mockPathArgument = Mock(YangInstanceIdentifier.NodeIdentifierWithPredicates)
            mockPathArgument.entrySet() >> [ ]
        expect: 'the result is an empty string'
            YangUtils.getKeyAttributesStatement(mockPathArgument) == ''
    }
}
