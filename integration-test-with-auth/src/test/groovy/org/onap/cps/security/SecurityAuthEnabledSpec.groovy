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
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
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
import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.MediaType.APPLICATION_JSON

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [org.onap.cps.api.CpsDataspaceService]
)
@Testcontainers
@EnableAutoConfiguration
@AutoConfigureTestRestTemplate
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

    @Shared
    static String validBearerToken

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    def setupSpec() {
        rsaKey = new RSAKeyGenerator(2048).keyID('test-key').generate()
        validBearerToken = 'Bearer ' + createSignedJwt()
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
        where: 'token presence determines access'
            scenario           | authorizationToken || expectedStatus
            'without a token'  | null               || HttpStatus.UNAUTHORIZED
            'with valid token' | validBearerToken   || HttpStatus.OK
    }

    def 'NCMP API security is enabled, request #scenario.'() {
        when: 'a request is made to an NCMP endpoint'
            def headers = new HttpHeaders()
            headers.setContentType(APPLICATION_JSON)
            if (authorizationToken != null) {
                headers.set('Authorization', authorizationToken)
            }
            def response = restTemplate.exchange(
                    new URI("http://localhost:${port}/ncmp/v1/ch/test-cm-handle"),
                    GET, new HttpEntity<>(headers), String)
        then: 'the expected status is returned'
            assert response.statusCode == expectedStatus
        where: 'token presence determines access to NCMP interface'
            scenario           | authorizationToken || expectedStatus
            'without a token'  | null               || HttpStatus.UNAUTHORIZED
            'with valid token' | validBearerToken   || HttpStatus.NOT_FOUND
    }

    def 'NCMP Inventory API security is enabled, request #scenario.'() {
        when: 'a request is made to an NCMP Inventory endpoint'
            def headers = new HttpHeaders()
            headers.setContentType(APPLICATION_JSON)
            if (authorizationToken != null) {
                headers.set('Authorization', authorizationToken)
            }
            def response = restTemplate.exchange(
                    new URI("http://localhost:${port}/ncmpInventory/v1/ch/cmHandles?dmi-plugin-identifier=test"),
                    GET, new HttpEntity<>(headers), String)
        then: 'the expected status is returned'
            assert response.statusCode == expectedStatus
        where: 'token presence determines access to NCMP Inventory interface'
            scenario           | authorizationToken || expectedStatus
            'without a token'  | null               || HttpStatus.UNAUTHORIZED
            'with valid token' | validBearerToken   || HttpStatus.OK
    }

    def 'ProvMnS API security is enabled, request #scenario.'() {
        when: 'a request is made to a ProvMnS endpoint'
            def headers = new HttpHeaders()
            headers.setContentType(APPLICATION_JSON)
            if (authorizationToken != null) {
                headers.set('Authorization', authorizationToken)
            }
            def response = restTemplate.exchange(
                    new URI("http://localhost:${port}/ProvMnS/v1/SubNetwork=test"),
                    GET, new HttpEntity<>(headers), String)
        then: 'the expected status is returned'
            assert response.statusCode == expectedStatus
        where: 'token presence determines access to ProvMnS interface'
            scenario           | authorizationToken || expectedStatus
            'without a token'  | null               || HttpStatus.UNAUTHORIZED
            'with valid token' | validBearerToken   || HttpStatus.NOT_FOUND
    }

    def 'ProvMnS Extensions API security is enabled, request #scenario.'() {
        when: 'a request is made to a ProvMnS Extensions endpoint'
            def headers = new HttpHeaders()
            headers.setContentType(APPLICATION_JSON)
            if (authorizationToken != null) {
                headers.set('Authorization', authorizationToken)
            }
            def response = restTemplate.exchange(
                    new URI("http://localhost:${port}/prov-mns-extensions/v1alpha1/actions/SubNetwork=test/reset"),
                    POST, new HttpEntity<>('{}', headers), String)
        then: 'the expected status is returned'
            assert response.statusCode == expectedStatus
        where: 'token presence determines access to ProvMnS Extensions interface'
            scenario           | authorizationToken || expectedStatus
            'without a token'  | null               || HttpStatus.UNAUTHORIZED
            'with valid token' | validBearerToken   || HttpStatus.NOT_FOUND
    }

    def 'NCMP request with URL-encoded alternate ID containing slashes.'() {
        given: 'an alternate ID with slashes, URL-encoded as a single path segment'
            def encodedAlternateId = URLEncoder.encode('/SubNetwork=Unknown/ManagedElement=Unknown', 'UTF-8')
        and: 'a request with a valid token'
            def headers = new HttpHeaders()
            headers.setContentType(APPLICATION_JSON)
            headers.set('Authorization', validBearerToken)
        when: 'a pass-through data request is made using the encoded alternate ID'
            def response = restTemplate.exchange(
                    new URI("http://localhost:${port}/ncmp/v1/ch/${encodedAlternateId}/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=id"),
                    GET, new HttpEntity<>(headers), String)
        then: 'the request is not rejected by Jetty or the security firewall (would be 400 Bad Request)'
            assert response.statusCode != HttpStatus.BAD_REQUEST
        and: 'instead it reaches the controller which reports the (unregistered) cm handle as not found'
            assert response.statusCode == HttpStatus.NOT_FOUND
    }

    static def createSignedJwt() {
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
