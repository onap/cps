/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

package org.onap.cps.utils

import org.onap.cps.api.exceptions.DataValidationException
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import spock.lang.Specification

class JsonParseFailureTranslatorSpec extends Specification {

    def mockLeafSchemaNode = Mock(LeafSchemaNode)

    def 'Returns the expected validation exception for a specific rule match #scenario.'() {
        given: 'A failure context for a leaf node'
            def context = new JsonNodeFailureContext(exception, mockLeafSchemaNode, xpath, nodeName, lastStringValue, lastValueNull, Set.of())
        when: 'The failure context is converted into a validation exception'
            def result = JsonParseFailureTranslator.translate(context)
        then: 'expected exception is thrown'
            assert result.class == DataValidationException
        and: 'The exception contains the expected message and details'
            assert result.message == 'Data Validation Failed'
            assert result.details == expectedDetails
        where: 'The following specific-rule failure scenarios are provided'
            scenario          | exception                                                                                           | xpath                               | nodeName         | lastStringValue | lastValueNull || expectedDetails
            'null leaf'       | new NullPointerException()                                                                          | '/bookstore/bookstore-name'         | 'bookstore-name' | null            | true          || "Validation failed at xpath '/bookstore/bookstore-name' : Null value is not allowed for mandatory leaf 'bookstore-name'."
            'invalid integer' | new IllegalArgumentException('invalid value', new NumberFormatException('For input string: "two"')) | '/bookstore/categories/books/price' | 'price'          | 'two'           | false         || "Validation failed at xpath '/bookstore/categories/books/price' : Value 'two' is not a valid integer."
    }

    def 'Returns a fallback validation exception when no specific rule matches #scenario.'() {
        given: 'A failure context for which no specific validation rule applies'
            def context = new JsonNodeFailureContext(exception, schemaNode, 'some xpath', 'some node', null, lastValueNull, Set.of())
        when: 'The failure context is converted into a validation exception'
            def result = JsonParseFailureTranslator.translate(context)
        then: 'expected exception is thrown'
            assert result.class == DataValidationException
        and: 'The exception has the expected message'
            assert result.message == 'Data Validation Failed'
        and: 'The original exception is preserved as cause'
            result.cause.is(exception)
        where: 'The following fallback scenarios are provided'
            scenario                                          | schemaNode          | exception                                                                                           | lastValueNull
            'unresolved schema node'                          | null                | new NullPointerException('unexpected internal error')                                               | false
            'null-valued non-leaf node'                       | null                | new NullPointerException()                                                                          | true
    }
}
