package org.onap.cps.ncmp.api.impl.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [WebClientConfiguration.DmiProperties])
class WebClientConfigurationSpec extends Specification {

    @Autowired
    WebClientConfiguration.DmiProperties dmiProperties

    def objectUnderTest = new WebClientConfiguration()

    def 'DMI Properties.'() {
        expect: 'properties are set to values in test configuration yaml file'
            dmiProperties.authUsername == 'some-user'
            dmiProperties.authPassword == 'some-password'
    }

    def 'WebClientConfiguration Construction.'() {
        expect: 'the system can create an instance'
            new WebClientConfiguration() != null
    }

    def 'WebClientConfiguration Construction2.'() {
        given: ''
            def wc = objectUnderTest.webClient()
        expect: 'the system can create an instance'
            assert wc != null
            assert wc instanceof WebClient
    }
}
