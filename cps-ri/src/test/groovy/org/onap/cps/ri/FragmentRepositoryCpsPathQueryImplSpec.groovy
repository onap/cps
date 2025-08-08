
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.query.parser.QuerySelectWhere
import org.onap.cps.query.parser.QuerySelectWhereUtil
import org.onap.cps.ri.CpsDataPersistenceServiceImpl
import org.onap.cps.ri.models.AnchorEntity
import org.onap.cps.ri.models.DataspaceEntity
import org.onap.cps.ri.repository.AnchorRepository
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.ri.repository.FragmentQueryBuilder
import org.onap.cps.ri.repository.FragmentRepository
import org.onap.cps.ri.repository.FragmentRepositoryCpsPathQueryImpl
import org.onap.cps.ri.utils.SessionManager
import org.onap.cps.utils.JsonObjectMapper
import jakarta.persistence.Query
import spock.lang.Specification
class FragmentRepositoryCpsPathQueryImplSpec extends Specification {
    def fragmentQueryBuilder = Mock(FragmentQueryBuilder)
    def objectMapper = Mock(ObjectMapper)
    def jsonObjectMapper = new JsonObjectMapper(objectMapper)
    def querySelectWhereUtil = Mock(QuerySelectWhereUtil)
    def fragmentRepository = new FragmentRepositoryCpsPathQueryImpl(fragmentQueryBuilder, jsonObjectMapper)
    def setup() {
    }
    def "findCustomNodes with valid wildcard query and AND conditions"() {
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
        querySelectWhere.getSelectFields() >> ["title", "price"]
        querySelectWhereUtil.getQuerySelectWhere("title,price", whereConditions) >> querySelectWhere
        def query = Mock(Query)
        fragmentQueryBuilder.getCustomNodesQuery(xpath, selectFields, querySelectWhere) >> query
        def jsonResult = '{"title": "Far Horizons", "price": 1099, "author": "Robert"}'
        def resultMap = [title: "Far Horizons", price: 1099, author: "Robert"]
        query.getResultList() >> [jsonResult]
        objectMapper.readValue(jsonResult, Map) >> resultMap
        when: "findCustomNodes is called"
        def result = fragmentRepository.findCustomNodes(anchorId, xpath, selectFields, whereConditions, querySelectWhere)
        then: "The correct filtered response is returned"
        result == [[title: "Far Horizons", price: 1099]]
    }
    def "findCustomNodes with JSON parsing failure throws RuntimeException"() {
        given: "Valid inputs with invalid JSON result"
        def anchorId = 1L
        def xpath = "/bookstore"
        def selectFields = ["title", "price"]
        def whereConditions = "price > 10"
        def querySelectWhere = Mock(QuerySelectWhere)
        def condition = new QuerySelectWhere.WhereCondition("price", ">", 10)
        querySelectWhere.getWhereConditions() >> [condition]
        querySelectWhere.getWhereBooleanOperators() >> []
        querySelectWhere.hasWhereConditions() >> true
        querySelectWhere.getSelectFields() >> ["title", "price"]
        querySelectWhereUtil.getQuerySelectWhere("title,price", whereConditions) >> querySelectWhere
        def query = Mock(Query)
        fragmentQueryBuilder.getCustomNodesQuery(xpath, selectFields, querySelectWhere) >> query
        def jsonResult = "invalid-json"
        query.getResultList() >> [jsonResult]
        objectMapper.readValue(jsonResult, Map) >> { throw new JsonProcessingException("Invalid JSON") }
        when: "findCustomNodes is called"
        fragmentRepository.findCustomNodes(anchorId, xpath, selectFields, whereConditions, querySelectWhere)
        then: "RuntimeException is thrown"
        thrown(RuntimeException)
    }
}
class CpsDataPersistenceServiceImplSpec extends Specification {
    def dataspaceRepository = Mock(DataspaceRepository)
    def anchorRepository = Mock(AnchorRepository)
    def fragmentRepository = Mock(FragmentRepository)
    def objectMapper = Mock(ObjectMapper)
    def jsonObjectMapper = new JsonObjectMapper(objectMapper)
    def sessionManager = Mock(SessionManager)
    def cpsDataPersistenceService = new CpsDataPersistenceServiceImpl(dataspaceRepository, anchorRepository, fragmentRepository, jsonObjectMapper, sessionManager)
    def setupSpec() {
    }
    def "getCustomNodes with valid wildcard query and AND conditions"() {
        given: "Valid inputs with wildcard xpath and where conditions"
        def dataspaceName = "my-dataspace"
        def anchorName = "my-anchor"
        def xpath = "/bookstore%"
        def selectFields = ["title", "price"]
        def whereConditions = "price > 10 and title LIKE '%Horizons'"
        def dataspaceEntity = new DataspaceEntity(id: 1L, name: dataspaceName)
        def anchorEntity = new AnchorEntity(id: 1L, name: anchorName, dataspace: dataspaceEntity)
        def querySelectWhere = Mock(QuerySelectWhere)
        def condition1 = new QuerySelectWhere.WhereCondition("price", ">", 10)
        def condition2 = new QuerySelectWhere.WhereCondition("title", "LIKE", "%Horizons")
        querySelectWhere.getWhereConditions() >> [condition1, condition2]
        querySelectWhere.getWhereBooleanOperators() >> ["AND"]
        querySelectWhere.hasWhereConditions() >> true
        querySelectWhere.getSelectFields() >> ["title", "price"]
        dataspaceRepository.getByName(dataspaceName) >> dataspaceEntity
        anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName) >> anchorEntity
        QuerySelectWhereUtil.getQuerySelectWhere("title,price", whereConditions) >> querySelectWhere
        fragmentRepository.findCustomNodes(anchorEntity.id, xpath, selectFields, whereConditions, querySelectWhere) >> [[title: "Far Horizons", price: 1099]]
        when: "getCustomNodes is called"
        def result = cpsDataPersistenceService.getCustomNodes(dataspaceName, anchorName, xpath, selectFields, whereConditions)
        then: "The correct filtered response is returned"
    }
}
