/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.spi

import org.onap.cps.api.parameters.PaginationOption
import spock.lang.Specification

class PaginationOptionSpec extends Specification {

    def 'Pagination validation with: #scenario'() {
        given: 'pagination option with pageIndex and pageSize'
            def paginationOption = new PaginationOption(pageIndex, pageSize)
        expect: 'validation returns expected result'
            assert paginationOption.isValidPaginationOption() == expectedIsValidPaginationOption
        where: 'following parameters are used'
            scenario           | pageIndex | pageSize || expectedIsValidPaginationOption
            'valid pagination' | 1         | 1        || true
            'negative index'   | -1        | 1        || false
            'negative size'    | 1         | -1       || false
            'zero index'       | 0         | 1        || false
            'zero size'        | 1         | 0        || false
    }
}
