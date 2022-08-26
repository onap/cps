package org.onap.cps.ncmp.api.utils

import org.onap.cps.ncmp.api.impl.utils.YangDataConverter
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class YangDataConverterSpec extends Specification{

    def 'Convert a cm handle data node with private properties.'() {
        given: 'a datanode with some additional (dmi, private) properties'
            def dataNodeAdditionalProperties = new DataNode(xpath:'/additional-properties[@name="dmiProp1"]',
                leaves: ['name': 'dmiProp1', 'value': 'dmiValue1'])
            def dataNode = new DataNode(childDataNodes:[dataNodeAdditionalProperties])
        when: 'the dataNode is converted'
            def yangModelCmHandle = YangDataConverter.convertCmHandleToYangModel(dataNode,'sample-id')
        then: 'the converted object has teh correct id'
            assert yangModelCmHandle.id == 'sample-id'
        and: 'the additional (dmi, private) properties are included'
            assert yangModelCmHandle.dmiProperties[0].name == 'dmiProp1'
            assert yangModelCmHandle.dmiProperties[0].value == 'dmiValue1'
    }
}
