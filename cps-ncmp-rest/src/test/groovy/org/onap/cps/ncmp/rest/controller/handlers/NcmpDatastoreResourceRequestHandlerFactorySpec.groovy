package org.onap.cps.ncmp.rest.controller.handlers

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
import spock.lang.Shared
import spock.lang.Specification

class NcmpDatastoreResourceRequestHandlerFactorySpec extends Specification {

    public static final int TIMEOUT_IN_MS = 0
    public static final boolean NOTIFICATION_ENABLED = false

    @Shared
    NetworkCmProxyDataService mockNetworkCmProxyDataService = Mock()

    @Shared
    CpsNcmpTaskExecutor mockCpsTaskExecutor = Mock()

    def objectUnderTest = new NcmpDatastoreResourceRequestHandlerFactory(
        mockNetworkCmProxyDataService, mockCpsTaskExecutor)

    def 'Factory get handler called with datastoreName it should return the right handler.'() {
        when: 'When #datastoreName is given and get handler is called'
            def newHandler = objectUnderTest
                .getNcmpDatastoreResourceRequestHandler(datastoreName)

        then: 'newHandler should be the right #handler'
            newHandler.getClass() == handler.getClass()

        where: 'the Factory has to build the following handler for each datastore'
            datastoreName                         | handler
            DatastoreType.OPERATIONAL             | new NcmpDatastoreOperationalResourceRequestHandler(
                mockNetworkCmProxyDataService, mockCpsTaskExecutor, TIMEOUT_IN_MS, NOTIFICATION_ENABLED)
            DatastoreType.PASSTHROUGH_OPERATIONAL | new NcmpDatastorePassthroughOperationalResourceRequestHandler(
                mockNetworkCmProxyDataService, mockCpsTaskExecutor, TIMEOUT_IN_MS, NOTIFICATION_ENABLED)
            DatastoreType.PASSTHROUGH_RUNNING     | new NcmpDatastorePassthroughRunningResourceRequestHandler(
                mockNetworkCmProxyDataService, mockCpsTaskExecutor, TIMEOUT_IN_MS, NOTIFICATION_ENABLED)
    }
}
