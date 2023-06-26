package org.onap.cps.ncmp.api.impl.utils.context

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification;

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
@ContextConfiguration(classes = [CpsApplicationContext.class])
class CpsApplicationContextSpec extends Specification {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Verify if cps application context contains a requested bean.'() {
        when: 'cps bean is requested from application context'
            def jsonObjectMapper = CpsApplicationContext.getCpsBean(JsonObjectMapper.class)
        then: 'requested bean of JsonObjectMapper is not null'
            assert jsonObjectMapper != null
    }
}
