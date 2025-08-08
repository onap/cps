/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd
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
 *  SPDX-License-LicenseIdentifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.query.parser

import org.onap.cps.cpspath.parser.PathParsingException
import spock.lang.Specification

class QuerySelectWhereBuilderSpec extends Specification {

    def builder = new QuerySelectWhereBuilder()

    def 'unwrapQuotedString with various inputs'() {
        expect: 'unwrapQuotedString processes input correctly'
        QuerySelectWhereBuilder.unwrapQuotedString(input) == expected
        where:
        input         | expected
        "'value'"     | 'value'
        "'val''ue'"   | "val'ue"
        "'abc'"       | 'abc'
    }

    def 'build returns QuerySelectWhere instance'() {
        given: 'A builder with populated fields'
        builder.exitSelectClause(null)
        builder.exitWhereClause(null)
        when: 'build is called'
        def result = builder.build()
        then: 'Result is a QuerySelectWhere instance'
        result instanceof QuerySelectWhere
        result.getSelectFields() == []
        result.getWhereConditions() == []
        result.getWhereBooleanOperators() == []
    }

    def 'Parse query with valid inputs: #scenario'() {
        when: 'Parsing query with select and where clauses'
        def result = QuerySelectWhereUtil.getQuerySelectWhere(select, where)
        then: 'Result has expected fields, conditions, and operators'
        result.getSelectFields() == expectedSelectFields
        result.getWhereBooleanOperators() == expectedBooleanOperators
        if (expectedWhereConditions != null) {
            result.getWhereConditions().size() == expectedWhereConditions.size()
            result.getWhereConditions().eachWithIndex { condition, i ->
                with(condition) {
                    name == expectedWhereConditions[i].name
                    operator == expectedWhereConditions[i].operator
                    value == expectedWhereConditions[i].value
                }
            }
        } else {
            result.getWhereConditions() == null || result.getWhereConditions().isEmpty()
        }
        where:
        scenario                     | select              | where                                   | expectedSelectFields | expectedWhereConditions                                                                 | expectedBooleanOperators
        'select only'               | 'field1,field2'    | null                                    | ['field1', 'field2'] | null                                                                                     | null
        'select with empty where'   | 'field1'           | ''                                      | ['field1']           | null                                                                                     | null
        'select with decimal condition' | 'field1'       | 'field5 <= 3.14'                       | ['field1']           | [new QuerySelectWhere.WhereCondition('field5', '<=', 3.14)]                             | []
        'select with single condition' | 'field1,field2' | 'field3 = 3'                     | ['field1', 'field2'] | [new QuerySelectWhere.WhereCondition('field3', '=', 3)]                           | []
        'select with integer condition' | 'field1'       | 'field4 > 42'                          | ['field1']           | [new QuerySelectWhere.WhereCondition('field4', '>', 42)]                                | []
        'select with multiple conditions' | 'field1,field2' | 'field3 = 2 and field4 = 10' | ['field1', 'field2'] | [new QuerySelectWhere.WhereCondition('field3', '=', 2), new QuerySelectWhere.WhereCondition('field4', '=', 10)] | ['and']
        'string with escaped quote' | 'field1'           | "field6 = 'val''ue'"                   | ['field1']           | [new QuerySelectWhere.WhereCondition('field6', '=', "val'ue")]                          | []
    }

    def 'Parse query with invalid inputs: #scenario'() {
        when: 'Parsing query with invalid inputs'
        QuerySelectWhereUtil.getQuerySelectWhere(select, where)
        then: 'PathParsingException is thrown'
        thrown(PathParsingException)
        where:
        scenario            | select      | where
        'empty select'     | ''          | null
        'invalid value'    | 'field1'    | 'field3 = true'
        'incomplete predicate' | 'field1' | 'field3 ='
    }
}