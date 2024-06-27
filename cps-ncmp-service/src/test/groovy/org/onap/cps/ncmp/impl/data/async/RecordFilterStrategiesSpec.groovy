package org.onap.cps.ncmp.impl.data.async

import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.Headers
import spock.lang.Specification

import java.nio.charset.Charset

class RecordFilterStrategiesSpec extends Specification {

    def objectUnderTest = new RecordFilterStrategies()

    def headers = Mock(Headers)
    def header = Mock(Header)

    def 'Determining cloud event using ce_type header for a #scenario.'() {
        given: 'headers contain a header for key: #key'
            headers.lastHeader(key) >> header
        expect: 'the check for cloud events returns #expectedResult'
            assert objectUnderTest.isCloudEvent(headers) == expectedResult
        where: 'the following headers (keys) are defined'
            scenario          | key       || expectedResult
            'cloud event'     | 'ce_type' || true
            'non-cloud event' | 'other'   || false
    }

    def 'Excluding cloud event of given type only with  #scenario.'() {
        given: 'headers contain a header for key: #key and value: #value'
            header.value() >> value.getBytes(Charset.defaultCharset())
            headers.lastHeader(key) >> header
        expect: 'the event would (not) be excluded: #expectedToBeExcluded'
            assert objectUnderTest.isNotCloudEventOfType(headers,'requiredType') == expectedToBeExcluded
        where: 'the following headers are defined'
            scenario                | key       | value                            || expectedToBeExcluded
            'required type'         | 'ce_type' | 'requiredType'                   || false
            'contains requiredType' | 'ce_type' | 'Contains requiredType and more' || false
            'other type'            | 'ce_type' | 'other'                          || true
            'no ce_type header'     | 'other'   | 'irrelevant'                     || true
    }

}
