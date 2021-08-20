package org.onap.cps.ncmp.api.models

import org.onap.cps.spi.model.ModuleReference
import spock.lang.Specification

class moduleReferenceSpec extends Specification {

    def NO_NAMESPACE = null

    def 'lombok data comtains expected methods'() {
        given: 'two moduleReference objects'
            def moduleReference1 = new ModuleReference('module1', NO_NAMESPACE, '1')
            def moduleReference2 = new ModuleReference('module1', NO_NAMESPACE, '1')
        when: 'lombok generated methods are called'
        then: 'the methods exist and behaviour is accurate'
            assert moduleReference1.toString() == moduleReference2.toString()
            assert moduleReference1 == moduleReference2
            assert moduleReference1.equals(moduleReference2)
            assert moduleReference1.hashCode() == moduleReference2.hashCode()
    }

}
