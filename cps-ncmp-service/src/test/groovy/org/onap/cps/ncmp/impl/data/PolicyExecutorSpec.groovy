package org.onap.cps.ncmp.impl.data


import org.onap.cps.ncmp.config.PolicyExecutorHttpClientConfig
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.policyexecutor.PolicyExecutorWebClientConfiguration
import org.onap.cps.ncmp.utils.WebClientBuilderTestConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [PolicyExecutor, PolicyExecutorWebClientConfiguration,  PolicyExecutorHttpClientConfig, WebClientBuilderTestConfig ])
class PolicyExecutorSpec extends Specification {

    @Autowired
    PolicyExecutor objectUnderTest

    def 'Policy Execution Configuration properties.'() {
        expect: 'properties used from application.yml'
            assert objectUnderTest.enabled
            assert objectUnderTest.serverAddress == 'http://localhost'
            assert objectUnderTest.serverPort == '8785'
    }
}
