/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 Wipro Limited.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.tdmt.client;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.onap.cps.tdmt.exception.CpsException;
import org.onap.cps.tdmt.model.AppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(AppConfiguration.class)
@TestPropertySource("classpath:application-test.properties")
public class CpsRestClientTest {

    @TestConfiguration
    static class CpsRestClientTestContextConfiguration {

        @Bean
        public CpsRestClient cpsRestClient() {
            return new CpsRestClient();
        }
    }

    @Autowired
    private CpsRestClient cpsRestClient;

    @MockBean
    private RestTemplate restTemplate;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testFetchNode() throws Exception {
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        final ResponseEntity<String> response = new ResponseEntity<>("sample response", responseHeaders,
            HttpStatus.OK);
        Mockito.when(restTemplate.exchange(ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenReturn(response);
        assertEquals("sample response", cpsRestClient.fetchNode("coverage-area-onap", "sample"));

        final ResponseEntity<String> errorResponse = new ResponseEntity<>("sample response",
            responseHeaders, HttpStatus.NOT_FOUND);
        Mockito.when(restTemplate.exchange(ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenReturn(errorResponse);
        exception.expect(CpsException.class);
        exception.expectMessage("Response code from CPS other than 200: 404");
        cpsRestClient.fetchNode("coverage-area-onap", "sample");

    }

    @Test
    public void testFetchNodeException() throws Exception {
        Mockito.when(restTemplate.exchange(ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenThrow(new RestClientException("Connection refused"));
        exception.expect(CpsException.class);
        exception.expectMessage("Connection refused");
        cpsRestClient.fetchNode("coverage-area-onap", "sample");
    }
}
