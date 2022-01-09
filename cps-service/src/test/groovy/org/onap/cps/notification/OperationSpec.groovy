package org.onap.cps.notification

import spock.lang.Specification

class OperationSpec extends Specification {

    def 'RootNode operation Validation'() {
        expect: 'operation has expected root node operation'
            operation.getRootNodeOperation() == expectedRootNodeOperation
        where:
            operation                   || expectedRootNodeOperation
            Operation.ROOT_NODE_CREATE  || 'create'
            Operation.ROOT_NODE_UPDATE  || 'update'
            Operation.ROOT_NODE_DELETE  || 'delete'
            Operation.CHILD_NODE_CREATE || 'update'
            Operation.ROOT_NODE_UPDATE  || 'update'
            Operation.ROOT_NODE_DELETE  || 'delete'
    }

}
