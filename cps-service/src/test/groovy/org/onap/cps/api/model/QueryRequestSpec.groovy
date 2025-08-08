package org.onap.cps.api.model

import spock.lang.Specification

class QueryRequestSpec extends Specification {

    def 'Set where condition in QueryRequest for #scenario.'() {
        given: 'A new QueryRequest instance'
        def queryRequest = new QueryRequest()

        when: 'setWhere is called with a condition'
        queryRequest.setWhere(whereCondition)

        then: 'The condition is correctly set and retrievable via getCondition'
        queryRequest.getCondition() == expectedCondition

        where: 'The following conditions are tested'
        scenario                   | whereCondition         | expectedCondition
        'valid condition'         | 'field1 = "value1"'    | 'field1 = "value1"'
        'null condition'          | null                   | null
        'empty condition'         | ''                     | ''
    }
}
