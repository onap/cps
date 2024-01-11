package org.onap.cps.ncmp.api.impl.config.embeddedcache

import com.hazelcast.core.Hazelcast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [AlternateIdCacheConfig])
class AlternateIdCacheConfigSpec extends Specification {

    @Autowired
    private Map<String, String> cmHandleIdPerAlternateId;
    @Autowired
    private Map<String, String> alternateIdPerCmHandleId;

    def 'Embedded (hazelcast) cache for alternate id - cm handle id caches.'() {
        expect: 'system is able to create an instance of the Alternate ID Cache'
            assert null != cmHandleIdPerAlternateId
            assert null != alternateIdPerCmHandleId
        and: 'there are at least 2 instances'
            assert Hazelcast.allHazelcastInstances.size() > 1
        and: 'Alternate ID Caches are present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceAlternateIdPerCmHandleMap')
                    && Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceCmHandlePerAlternateIdMap')
    }

    def 'Verify configs of the alternate id distributed objects.'(){
        when: 'retrieving the map config of module set tag'
            def alternateIdConfig =  Hazelcast.getHazelcastInstanceByName('hazelcastInstanceAlternateIdPerCmHandleMap').config
            def alternateIdMapConfig = alternateIdConfig.mapConfigs.get('alternateIdToCmHandleCacheConfig')
            def cmHandleIdConfig =  Hazelcast.getHazelcastInstanceByName('hazelcastInstanceCmHandlePerAlternateIdMap').config
            def cmHandleIdIdMapConfig = cmHandleIdConfig.mapConfigs.get('cmHandleToAlternateIdCacheConfig')
        then: 'the map configs have the correct number of backups'
            assert alternateIdMapConfig.backupCount == 3
            assert alternateIdMapConfig.asyncBackupCount == 3
            assert cmHandleIdIdMapConfig.backupCount == 3
            assert cmHandleIdIdMapConfig.asyncBackupCount == 3
    }
}
