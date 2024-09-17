/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ri.utils

import spock.lang.Specification

class EscapeUtilsSpec extends Specification {

    def 'Escape text for use in SQL LIKE operation.'() {
        expect: 'SQL LIKE special characters to be escaped with forward-slash'
            assert EscapeUtils.escapeForSqlLike(unescapedText) == escapedText
        where:
            unescapedText                   || escapedText
            'Only %, _, and \\ are special' || 'Only \\%, \\_, and \\\\ are special'
            'Others (./?$) are not special' || 'Others (./?$) are not special'
    }

    def 'Escape text for use in SQL string literal.'() {
        expect: 'single quotes to be doubled'
            assert EscapeUtils.escapeForSqlStringLiteral("I'm escaping!") == "I''m escaping!"
    }

}
