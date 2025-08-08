/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2023 TechMahindra Ltd
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

package org.onap.cps.query.parser

import org.onap.cps.cpspath.parser.PathParsingException
import spock.lang.Specification

class QuerySelectWhereSpec extends Specification {

    def 'Getters and setters'() {
        given: 'A QuerySelectWhere instance'
        def querySelectWhere = new QuerySelectWhere()
        when: 'Setting fields'
        querySelectWhere.setSelectFields(['field1', 'field2'])
        querySelectWhere.setWhereConditions([
                new QuerySelectWhere.WhereCondition('field3', '=', 'value'),
                new QuerySelectWhere.WhereCondition('field4', '>', 5)
        ])
        querySelectWhere.setWhereBooleanOperators(['AND'])
        then: 'Getters return the set values'
        querySelectWhere.getSelectFields() == ['field1', 'field2']
        querySelectWhere.getWhereConditions() == [
                new QuerySelectWhere.WhereCondition('field3', '=', 'value'),
                new QuerySelectWhere.WhereCondition('field4', '>', 5)
        ]
        querySelectWhere.getWhereBooleanOperators() == ['AND']
    }

    def 'hasWhereConditions with different conditions'() {
        given: 'A QuerySelectWhere instance'
        def querySelectWhere = new QuerySelectWhere()
        when: 'Setting whereConditions to #conditions'
        querySelectWhere.setWhereConditions(conditions)
        then: 'hasWhereConditions returns #expectedResult'
        querySelectWhere.hasWhereConditions() == expectedResult
        where:
        conditions                                              | expectedResult
        null                                                    | false
        []                                                      | false
        [new QuerySelectWhere.WhereCondition('field', '=', 'value')] | true
    }

    def 'createFrom with valid queries: #scenario'() {
        when: 'Creating QuerySelectWhere from select and where clauses'
        def result = QuerySelectWhere.createFrom(select, where)
        then: 'Result has expected fields, conditions, and operators'
        result.getSelectFields() == expectedSelectFields
        result.getWhereConditions() == expectedWhereConditions
        result.getWhereBooleanOperators() == expectedBooleanOperators
        where:
        scenario                     | select              | where                                   | expectedSelectFields | expectedWhereConditions                                                                 | expectedBooleanOperators
        'select only'               | 'field1,field2'    | null                                    | ['field1', 'field2'] | null                                                                                     | null
        'select with empty where'   | 'field1'           | ''                                      | ['field1']           | null                                                                                    | null
        'select with single condition' | 'field1,field2' | 'field3 = 23'                     | ['field1', 'field2'] | [new QuerySelectWhere.WhereCondition('field3', '=', 23)]                           | []
        'select with integer condition' | 'field1'       | 'field4 > 42'                          | ['field1']           | [new QuerySelectWhere.WhereCondition('field4', '>', 42)]                                | []
        'select with multiple conditions' | 'field1,field2' | 'field3 = 8 and field4 = 10' | ['field1', 'field2'] | [new QuerySelectWhere.WhereCondition('field3', '=', 8), new QuerySelectWhere.WhereCondition('field4', '=', 10)] | ['and']
        'string with escaped quote' | 'field1'           | "field6 = 'val''ue'"                   | ['field1']           | [new QuerySelectWhere.WhereCondition('field6', '=', "val'ue")]                          | []
    }

    def 'createFrom with invalid queries: #scenario'() {
        when: 'Creating QuerySelectWhere with invalid inputs'
        QuerySelectWhere.createFrom(select, where)
        then: 'PathParsingException is thrown'
        thrown(PathParsingException)
        where:
        scenario            | select      | where
        'empty select'     | ''          | null
        'invalid value'    | 'field1'    | 'field3 = true'
        'incomplete predicate' | 'field1' | 'field3 ='
    }
}