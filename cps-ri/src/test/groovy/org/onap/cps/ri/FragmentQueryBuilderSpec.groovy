package org.onap.cps.ri

import org.onap.cps.cpspath.parser.PathParsingException
import org.onap.cps.query.parser.QuerySelectWhere
import org.onap.cps.query.parser.QuerySelectWhereUtil
import org.onap.cps.ri.repository.FragmentQueryBuilder
import spock.lang.Specification
import jakarta.persistence.EntityManager
import jakarta.persistence.Query

class FragmentQueryBuilderSpec extends Specification {

    def entityManager = Mock(EntityManager)
    def querySelectWhereUtil = Mock(QuerySelectWhereUtil)
    def fragmentQueryBuilder = new FragmentQueryBuilder(entityManager: entityManager)

    def setup() {
        fragmentQueryBuilder.entityManager = entityManager
    }

    def "getCustomNodesQuery with valid wildcard query and AND conditions"() {
        given: "Valid inputs with wildcard xpath and where conditions"
        def anchorId = 1L
        def xpath = "/bookstore%"
        def selectFields = ["title", "price"]
        def whereConditions = "price > 10 and title like '%Horizons'"
        def querySelectWhere = Mock(QuerySelectWhere)
        def condition1 = new QuerySelectWhere.WhereCondition("price", ">", 10)
        def condition2 = new QuerySelectWhere.WhereCondition("title", "LIKE", "%Horizons")
        querySelectWhere.getWhereConditions() >> [condition1, condition2]
        querySelectWhere.getWhereBooleanOperators() >> ["AND"]
        querySelectWhere.hasWhereConditions() >> true
        querySelectWhereUtil.getQuerySelectWhere("title,price", whereConditions) >> querySelectWhere
        def query = Mock(Query)
        entityManager.createNativeQuery("SELECT f.attributes FROM fragment f WHERE f.xpath LIKE ?1 AND CAST(f.attributes->>'price' AS DOUBLE PRECISION) > ?2 AND f.attributes->>'title' LIKE ?3") >> query
        query.setParameter(1, xpath) >> query
        query.setParameter(2, 10) >> query
        query.setParameter(3, "%Horizons") >> query
        when: "getCustomNodesQuery is called"
        def result = fragmentQueryBuilder.getCustomNodesQuery(xpath, selectFields, querySelectWhere)
        then: "The correct query is returned"
        result == query
    }

    def "getCustomNodesQuery with null xpath throws PathParsingException"() {
        given: "Null xpath"
        def anchorId = 1L
        def xpath = null
        def selectFields = ["title", "price"]
        def whereConditions = "price > 10"
        def querySelectWhere = Mock(QuerySelectWhere)
        when: "getCustomNodesQuery is called"
        fragmentQueryBuilder.getCustomNodesQuery(xpath, selectFields, querySelectWhere)
        then: "PathParsingException is thrown"
        thrown(PathParsingException)
    }

    def "getCustomNodesQuery with null selectFields throws IllegalArgumentException"() {
        given: "Null selectFields"
        def anchorId = 1L
        def xpath = "/bookstore"
        def selectFields = null
        def whereConditions = "price > 10"
        def querySelectWhere = Mock(QuerySelectWhere)
        when: "getCustomNodesQuery is called"
        fragmentQueryBuilder.getCustomNodesQuery(xpath, selectFields, querySelectWhere)
        then: "IllegalArgumentException is thrown"
        thrown(IllegalArgumentException)
    }

    def "getCustomNodesQuery with single non-numeric equality condition"() {
        given: "Valid inputs with a single non-numeric equality condition"
        def anchorId = 1L
        def xpath = "/bookstore"
        def selectFields = ["title", "price"]
        def whereConditions = "title = 'Far Horizons'"
        def querySelectWhere = Mock(QuerySelectWhere)
        def condition = new QuerySelectWhere.WhereCondition("title", "=", "Far Horizons")
        querySelectWhere.getWhereConditions() >> [condition]
        querySelectWhere.getWhereBooleanOperators() >> []
        querySelectWhere.hasWhereConditions() >> true
        querySelectWhereUtil.getQuerySelectWhere("title,price", whereConditions) >> querySelectWhere
        def query = Mock(Query)
        entityManager.createNativeQuery("SELECT f.attributes FROM fragment f WHERE f.xpath LIKE ?1 AND f.attributes->>'title' = ?2") >> query
        query.setParameter(1, xpath) >> query
        query.setParameter(2, "Far Horizons") >> query
        when: "getCustomNodesQuery is called"
        def result = fragmentQueryBuilder.getCustomNodesQuery(xpath, selectFields, querySelectWhere)
        then: "The correct query is returned"
        result == query
    }
}
