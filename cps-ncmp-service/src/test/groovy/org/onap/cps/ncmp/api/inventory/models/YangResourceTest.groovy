package org.onap.cps.ncmp.api.inventory.models


import spock.lang.Specification

class YangResourceSpec extends Specification {

    YangResource objectUnderTest = new YangResource(moduleName: 'module name',
                                                    revision:'revision',
                                                    yangSource:'source')

    def 'Yang resource attributes'() {
        expect: 'correct module name'
            objectUnderTest.moduleName == 'module name'
        and: 'correct revision (this property is not used in production code, hence the need for this test)'
            objectUnderTest.revision == 'revision'
        and: 'correct yang source'
            objectUnderTest.yangSource == 'source'
    }

}
