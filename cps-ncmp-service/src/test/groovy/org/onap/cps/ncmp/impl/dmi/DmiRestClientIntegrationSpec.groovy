package org.onap.cps.ncmp.impl.dmi

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.onap.cps.ncmp.api.exceptions.DmiClientRequestException
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.InvalidUrlException
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.READ
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA

class DmiRestClientIntegrationSpec extends Specification {

    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url('/')
    def dataServiceWebClient = WebClient.builder().baseUrl(baseUrl.toString()).build()

    def urlTemplateParameters = new UrlTemplateParameters('/myPath', [someParam: 'value'])
    def mockDmiServiceAuthenticationProperties = Mock(DmiServiceAuthenticationProperties)

    def mockModelServicesWebClient = Mock(WebClient)
    def mockHealthChecksWebClient = Mock(WebClient)

    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def objectUnderTest = new DmiRestClient(mockDmiServiceAuthenticationProperties, jsonObjectMapper, dataServiceWebClient, mockModelServicesWebClient, mockHealthChecksWebClient)

    def cleanup() throws IOException {
        mockWebServer.shutdown();
    }

    def 'Synchronous DMI #method request.'() {
        given: 'Web Server wil return OK response'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value))
        when: '#method request is made'
            def result
            switch(method) {
                case 'get':
                    result = objectUnderTest.synchronousGetOperation(DATA, urlTemplateParameters, READ)
                    break
                case 'post':
                    result = objectUnderTest.synchronousPostOperation(DATA, urlTemplateParameters, 'body', CREATE, '')
                    break
                case 'put':
                    result = objectUnderTest.synchronousPutOperation(DATA, urlTemplateParameters, UPDATE)
                    break
                case 'delete':
                    result = objectUnderTest.synchronousDeleteOperation(DATA, urlTemplateParameters)
            }
        then: 'the result has the same status code of 200'
            assert result.statusCode.value() == 200
        where: 'the following http methods are used'
            method << ['get', 'post', 'put', 'delete']
    }

    def 'Synchronous DMI #method request with invalid JSON.'() {
        given: 'Web Server wil return OK response'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value)
                .setBody('invalid-json:!!')
                .addHeader("Content-Type", "application/json"))
        when: 'a synchronous read request is made'
            switch(method) {
                case 'get':
                    objectUnderTest.synchronousGetOperation(DATA, urlTemplateParameters, READ)
                    break;
                case 'post':
                    objectUnderTest.synchronousPostOperation(DATA, urlTemplateParameters, 'body', READ, '')
                    break
                case 'put':
                    objectUnderTest.synchronousPutOperation(DATA, urlTemplateParameters, UPDATE)
                    break
                case 'delete':
                    objectUnderTest.synchronousDeleteOperation(DATA, urlTemplateParameters)
            }
            objectUnderTest.synchronousGetOperation(DATA, urlTemplateParameters, READ)
        then: 'a dmi client request exception is thrown with the correct error codes'
            def thrown = thrown(DmiClientRequestException)
            assert thrown.getHttpStatusCode() == 500
            assert thrown.ncmpResponseStatus.code == '108'
        where: 'the following http methods are used'
            method << ['get','post','put','delete']
    }

    def 'DMI Request with not responding server.'() {
        given: 'the web server is shut down'
            mockWebServer.shutdown()
        when: 'a synchronous read request is made'
            objectUnderTest.synchronousGetOperation(DATA, urlTemplateParameters, READ)
        then: 'a dmi client request exception is thrown with status code of 503 Service Unavailable'
            def thrown = thrown(DmiClientRequestException)
            assert thrown.getHttpStatusCode() == 503
    }

    def 'DMI Request with #scenario.'() {
        given: 'The mock server or exception setup'
            mockWebServer.enqueue(new MockResponse().setResponseCode(responseCode.value))
        when: 'A synchronous read request is made'
            objectUnderTest.synchronousGetOperation(DATA, urlTemplateParameters, READ)
        then: "The expected DmiClientRequestException is thrown with the right status"
            def thrown = thrown(DmiClientRequestException)
            assert thrown.httpStatusCode == expectedStatus
        where:
            scenario             | responseCode                   || expectedStatus
            'Client error (418)' | HttpStatus.I_AM_A_TEAPOT       || 418
            'Timeout (408)'      | HttpStatus.REQUEST_TIMEOUT     || 408
            'Server error (503)' | HttpStatus.SERVICE_UNAVAILABLE || 503
    }

    def 'DMI Request with unexpected runtime exception during mapping'() {
        given: 'Mock a bad URL that causes IllegalArgumentException before HTTP call'
            def badUrlParameters = new UrlTemplateParameters(':://bad url', [someParam: 'value'])
        when: 'a synchronous request is made'
            objectUnderTest.synchronousGetOperation(DATA, badUrlParameters, READ)
        then: 'a DmiClientRequestException is thrown (mapped from unexpected exception)'
            thrown(InvalidUrlException)
    }
}
