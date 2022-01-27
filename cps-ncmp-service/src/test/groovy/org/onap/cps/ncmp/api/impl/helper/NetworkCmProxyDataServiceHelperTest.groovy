package org.onap.cps.ncmp.api.impl.helper

import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

class NetworkCmProxyDataServiceHelperTest extends Specification {

	def noChildren = []

	def dataNode = buildDataNode("/dmi-registry/cm-handles[@id=myHandle1]", ['prop1': 'value1', 'prop2': 'value2', 'prop3': 'value3'], noChildren)

	static def buildDataNode(xpath, leaves, children) {
		return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(children).build()
	}

	def 'CM-Handle Registration update #scenario'() {
		when: 'dataNode is provided along with attributeKey and attributeValue to be updated'
			NetworkCmProxyDataServiceHelper.handleAddOrRemoveCmHandleProperties(dataNode, attributeKey, attributeValue)
		then: 'the values are updated,added or removed'
			dataNode.leaves[attributeKey] == expectedLeaveValue
			dataNode.leaves.size() == expectedSize
			dataNode.leaves.keySet().containsAll(expectedKeys)
		where:
			scenario                      | attributeKey | attributeValue || expectedLeaveValue | expectedKeys                         | expectedSize
			'attribute will be removed'   | 'prop1'      | null           || null               | ['prop2', 'prop3']                   | 2
			'attribute value is updated'  | 'prop2'      | 'newValue2'    || 'newValue2'        | ['prop1', 'prop2', 'prop3']          | 3
			'new Attribute will be added' | 'prop4'      | 'value4'       || 'value4'           | ['prop1', 'prop2', 'prop3', 'prop4'] | 4
			'request will be ignored'     | 'prop5'      | null           || null               | ['prop1', 'prop2', 'prop3']          | 3


	}

}
