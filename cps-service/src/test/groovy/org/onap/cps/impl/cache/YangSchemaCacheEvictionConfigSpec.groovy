package org.onap.cps.impl.cache

import com.hazelcast.core.Hazelcast
import com.hazelcast.topic.ITopic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [YangSchemaCacheEvictionConfig])
class YangSchemaCacheEvictionConfigSpec extends Specification {

    @Autowired
    ITopic<String> yangSchemaCacheEvictionTopic

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').shutdown()
    }

    def 'Yang schema cache eviction topic.'() {
        expect: 'system is able to create an instance of the Yang schema cache eviction topic'
            assert null != yangSchemaCacheEvictionTopic
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Hazelcast cache instance for yang schema cache eviction topic returns the same bean'
            assert Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').getTopic('yangSchemaCacheEvictionTopic') == yangSchemaCacheEvictionTopic
    }

}
