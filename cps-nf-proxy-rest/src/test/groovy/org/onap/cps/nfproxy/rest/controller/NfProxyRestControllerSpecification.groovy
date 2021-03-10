package org.onap.cps.nfproxy.rest.controller

import spock.lang.Specification

class NfProxyRestControllerSpecification extends Specification {

    def authorizationHeader = 'Basic Y3BzdXNlcjpjcHNyMGNrcyE='

    def getAuthorizationHeader() {
        return authorizationHeader
    }

}
