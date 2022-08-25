package org.onap.cps.ncmp.rest.controller.handlers

import spock.lang.Specification

class NcmpDatastoreResourceRequestHandlerFactorySpec extends Specification {

    def objectUnderTest = new NcmpDatastoreResourceRequestHandlerFactory(null, null)

    def 'Creating ncmp datastore request handlers.'() {
        when: 'a ncmp datastore request handler is create for #datastoreType'
            def result = objectUnderTest.getNcmpDatastoreResourceRequestHandler(datastoreType)
        then: 'the result is of the expected class'
            result.class == expectedClass
        where: 'the following type of datastore is used'
            datastoreType                         || expectedClass
            DatastoreType.OPERATIONAL             || NcmpDatastoreOperationalResourceRequestHandler
            DatastoreType.PASSTHROUGH_OPERATIONAL || NcmpDatastorePassthroughOperationalResourceRequestHandler
            DatastoreType.PASSTHROUGH_RUNNING     || NcmpDatastorePassthroughRunningResourceRequestHandler
    }
}