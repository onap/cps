package org.onap.cps.ncmp.rest.util


import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter
import org.onap.cps.ncmp.impl.provmns.model.Scope
import spock.lang.Specification

class ProvMnSParametersMapperSpec extends Specification{

    def objectUnderTest = new ProvMnSParametersMapper()

    def 'Extract url template parameters for GET'() {
        when:'a set of given parameters from a call are passed in'
            def result = objectUnderTest.getUrlTemplateParameters(new Scope(scopeLevel: 1, scopeType: "BASE_ALL"),
                    "some-filter", ["some-attribute"], ["some-field"], new ClassNameIdGetDataNodeSelectorParameter(dataNodeSelector: "some-dataSelector"),
                    new YangModelCmHandle(dmiServiceName: 'some-dmi-service'))
        then:'verify object has been mapped correctly'
            result.urlVariables().get('filter') == 'some-filter'
    }

    def 'Check if dataProducerIdentifer is populated'() {
        given:'a yangModelCmHandle'
            def yangModelCmHandle = new YangModelCmHandle(dataProducerIdentifier: 'some-dataProducer-ID')
        when:'a yangModelCmHandle is passed in'
            def result = objectUnderTest.checkDataProducerIdentifier(yangModelCmHandle)
        then: 'no exception thrown for yangModelCmHandle'
            noExceptionThrown()
    }
}
