package org.onap.cps.ncmp.api.impl.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [DmiWebClientConfiguration.DmiProperties])
class DmiWebClientConfigurationSpec extends Specification {

    @Autowired
    DmiWebClientConfiguration.DmiProperties dmiProperties

    def objectUnderTest = new DmiWebClientConfiguration()

    def 'DMI Properties.'() {
        expect: 'properties are set to values in test configuration yaml file'
            dmiProperties.authUsername == 'some-user'
            dmiProperties.authPassword == 'some-password'
    }

    def 'Web Client Configuration construction.'() {
        expect: 'the system can create an instance'
            new DmiWebClientConfiguration() != null
    }

    def 'Creating a WebClint instance.'() {
        given: 'WebClient configuration invoked'
            def webClientInstance = objectUnderTest.webClient()
        expect: 'the system can create an instance'
            assert webClientInstance != null
            assert webClientInstance instanceof WebClient
    }
}
