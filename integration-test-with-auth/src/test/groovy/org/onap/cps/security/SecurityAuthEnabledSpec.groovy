/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.onap.cps.ri.repository.DataspaceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.MediaType.APPLICATION_JSON

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [org.onap.cps.api.CpsDataspaceService]
)
@Testcontainers
@EnableAutoConfiguration
@EnableJpaRepositories(basePackageClasses = [DataspaceRepository])
@ComponentScan(basePackages = ['org.onap.cps'])
@EntityScan('org.onap.cps.ri.models')
class SecurityAuthEnabledSpec extends Specification {

    @Shared
    def JWKS_SERVER_PORT = 9876

    @Shared
    DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance()

    @Shared
    KafkaTestContainer kafkaTestContainer = KafkaTestContainer.getInstance()

    @Shared
    static MockWebServer mockJwksServer

    @Shared
    static RSAKey rsaKey

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    def setupSpec() {
        rsaKey = new RSAKeyGenerator(2048).keyID('test-key').generate()
        mockJwksServer = new MockWebServer()
        mockJwksServer.setDispatcher(new Dispatcher() {
            @Override
            MockResponse dispatch(RecordedRequest request) {
                def jwks = '{"keys":[' + rsaKey.toPublicJWK().toJSONString() + ']}'
                return new MockResponse().setBody(jwks).setHeader('Content-Type', 'application/json')
            }
        })
        mockJwksServer.start(JWKS_SERVER_PORT)
    }

    def cleanupSpec() {
        mockJwksServer?.shutdown()
    }

    def 'CPS API security is enabled, request #scenario.'() {
        when: 'a request is made to a CPS Core endpoint'
            def headers = new HttpHeaders()
            headers.setContentType(APPLICATION_JSON)
            if (authorizationToken != null) {
                headers.set('Authorization', authorizationToken)
            }
            def response = restTemplate.exchange(
                    new URI("http://localhost:${port}/cps/api/v2/dataspaces/NCMP-Admin/anchors"),
                    GET, new HttpEntity<>(headers), String)
        then: 'the expected status is returned'
            assert response.statusCode == expectedStatus
        where:
            scenario           | authorizationToken            || expectedStatus
            'without a token'  | null                          || HttpStatus.UNAUTHORIZED
            'with valid token' | 'Bearer ' + createSignedJwt() || HttpStatus.OK
    }

    def createSignedJwt() {
        def claims = new JWTClaimsSet.Builder()
                .subject('test-user')
                .issuer('test-issuer')
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build()
        def signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(), claims)
        signedJwt.sign(new RSASSASigner(rsaKey))
        return signedJwt.serialize()
    }
}
