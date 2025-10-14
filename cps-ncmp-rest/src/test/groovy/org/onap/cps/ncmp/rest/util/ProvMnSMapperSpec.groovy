package org.onap.cps.ncmp.rest.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.rest.provmns.model.request.Operation
import org.onap.cps.ncmp.rest.provmns.model.request.Value
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class ProvMnSMapperSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    
    def 'Transforming 3gpp requests - #scenario.'() {
        given: 'an incoming patch request'
        when: 'converted into the new format'
            def result = ProvMnSMapper.mapOperations([request], '/ProvMnS')
        then: 'result is the expected'
            assert result.permissionId != null
            assert result.changeRequestFormat == 'cm-legacy'
            assert expected == jsonObjectMapper.asJsonString(result.operations)
        where:
            scenario                     | request            || expected
            'mapping a create operation' | getCreateRequest() || '[{"operation":"create","targetIdentifier":"/ProvMnS/ManagedElement=ME3","changeRequests":{"ManagedElement":[{"id":"ME3","attributes":{"userLabel":"Berlin NW 3"}}]}}]'
            'mapping a delete operation' | getDeleteRequest() || '[{"operation":"delete","targetIdentifier":"/ProvMnS/ManagedElement=ME3/XyzFunction=XYZF2","changeRequests":null}]'

    }

    def getCreateRequest() {
        return new Operation(op: 'add', path: '/ManagedElement=ME3', value: new Value(id: 'ME3', objectClass: 'ManagedElement', attributes: [userLabel: 'Berlin NW 3']))
    }

    def getDeleteRequest() {
        return new Operation(op: 'remove', path: '/ManagedElement=ME3/XyzFunction=XYZF2', value: new Value(id: 'XYZF2', objectClass: 'XyzFunction', attributes: ['attrA': 'abc', 'attrB': 771]))
    }

}
