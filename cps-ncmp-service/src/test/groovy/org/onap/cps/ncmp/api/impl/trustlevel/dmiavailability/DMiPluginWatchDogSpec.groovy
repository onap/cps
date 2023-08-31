package org.onap.cps.ncmp.api.impl.trustlevel.dmiavailability

import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.impl.config.embeddedcache.TrustLevelCacheConfig;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [TrustLevelCacheConfig, DMiPluginWatchDog])
class DMiPluginWatchDogSpec extends Specification {

    @Autowired
    IMap<String, TrustLevel> trustLevelPerDmiPlugin
    @SpringBean
    DmiRestClient mockDmiRestClient = Mock(DmiRestClient)
    @Autowired
    DMiPluginWatchDog objectUnderTest

    def setup() {
        trustLevelPerDmiPlugin.put('dmi1', TrustLevel.NONE)
        trustLevelPerDmiPlugin.put('dmi2', TrustLevel.NONE)
    }

    def 'watch dmi plugin aliveness #scenario'() {
        given: 'the dmi client returns aliveness #scenario'
            mockDmiRestClient.getDmiPluginStatus('dmi1') >> dmi1Status
            mockDmiRestClient.getDmiPluginStatus('dmi2') >> dmi2Status
        when: 'watch dog started'
            objectUnderTest.watchDmiPluginAliveness()
        then: 'get trust level as per dmi'
            trustLevelPerDmiPlugin.get('dmi1') == dmi1TrustLevel
            trustLevelPerDmiPlugin.get('dmi2') == dmi2TrustLevel
        where: 'the following parameter are used'
            scenario                            | dmi1Status            | dmi2Status            || dmi1TrustLevel       ||  dmi2TrustLevel
            'dmi1 and dmi2 are UP'              | DmiPluginStatus.UP    | DmiPluginStatus.UP    || TrustLevel.COMPLETE  ||  TrustLevel.COMPLETE
            'dmi1 and dmi2 are DOWN'            | DmiPluginStatus.DOWN  | DmiPluginStatus.DOWN  || TrustLevel.NONE      ||  TrustLevel.NONE
            'dmi1 is UP and dmi2 is DOWN'       | DmiPluginStatus.UP    | DmiPluginStatus.DOWN  || TrustLevel.COMPLETE  ||  TrustLevel.NONE
            'dmi1 is DOWN and dmi2 is UP'       | DmiPluginStatus.DOWN  | DmiPluginStatus.UP    || TrustLevel.NONE      ||  TrustLevel.COMPLETE
    }
}
