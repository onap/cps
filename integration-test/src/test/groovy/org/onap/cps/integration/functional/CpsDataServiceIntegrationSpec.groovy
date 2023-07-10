/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional

import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.AlreadyDefinedExceptionBatch
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.CpsAdminException
import org.onap.cps.spi.exceptions.CpsPathException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException

import java.time.OffsetDateTime

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataService objectUnderTest
    def originalCountBookstoreChildNodes
    def originalCountParentlistNodes
    def now = OffsetDateTime.now()

    def setup() {
        objectUnderTest = cpsDataService
        originalCountBookstoreChildNodes = countDataNodesInBookstore()
        originalCountParentlistNodes = countDataNodesInParentlist()
    }

    def 'Read bookstore top-level container(s) using #fetchDescendantsOption.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', fetchDescendantsOption)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert countDataNodesInTree(result) == expectNumberOfDataNodes
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons']
        and: 'they are from the correct dataspace'
            assert result.dataspace == [FUNCTIONAL_TEST_DATASPACE_1]
        and: 'they are from the correct anchor'
            assert result.anchorName == [BOOKSTORE_ANCHOR_1]
        where: 'the following option is used'
            fetchDescendantsOption        || expectNumberOfDataNodes
            OMIT_DESCENDANTS              || 1
            DIRECT_CHILDREN_ONLY          || 6
            INCLUDE_ALL_DESCENDANTS       || 17
            new FetchDescendantsOption(2) || 17
    }

    def 'Read parent-list top-level using "root"  variations.'() {
        when: 'get data nodes for parent-list top-level'
            def result = objectUnderTest.getDataNodes(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, root, OMIT_DESCENDANTS)
        then: 'the list consist ouf of one data node'
            assert countDataNodesInTree(result) == 1
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['ProductID'] == [1]
        where: 'the following variations of "root" are used'
            root << ['/', '']
    }

    def 'Read bookstore top-level container(s) using "root" path variations.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, root, OMIT_DESCENDANTS)
        then: 'the tree consist ouf of one data node'
            assert countDataNodesInTree(result) == 1
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons']
        where: 'the following variations of "root" are used'
            root << [ '/', '' ]
    }

    def 'Read data nodes with error: #cpsPath'() {
        when: 'attempt to get data nodes using invalid path'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, DIRECT_CHILDREN_ONLY)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where:
            cpsPath              || expectedException
            'invalid path'       || CpsPathException
            '/non-existing-path' || DataNodeNotFoundException
    }

    def 'Read (multiple) data nodes (batch) with #cpsPath'() {
        when: 'attempt to get data nodes using invalid path'
            objectUnderTest.getDataNodesForMultipleXpaths(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, [ cpsPath ], DIRECT_CHILDREN_ONLY)
        then: 'no exception is thrown'
            noExceptionThrown()
        where:
            cpsPath << [ 'invalid path', '/non-existing-path' ]
    }

    def 'Delete root data node.'() {
        when: 'the "root" is deleted'
            objectUnderTest.deleteDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, [ '/' ], now)
        and: 'attempt to get the top level data node'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', DIRECT_CHILDREN_ONLY)
        then: 'an datanode not found exception is thrown'
            thrown(DataNodeNotFoundException)
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Add and Delete top-level list (element) data nodes with root node.'() {
        given: 'two new (categories) datanodes'
            def json = '{"multiple-data-tree:invoice": [{"ProductID": "2","ProductName": "Mango","price": "150","stock": true}]}'
        when: 'the new list elements are saved'
            objectUnderTest.saveData(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, json, OffsetDateTime.now())
        then: 'they can be retrieved by their xpaths'
            objectUnderTest.getDataNodes(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, '/invoice[@ProductID ="2"]', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS).size() == 1
        and: 'there are now two extra datanodes'
            assert originalCountParentlistNodes + 1 == countDataNodesInTree(objectUnderTest.getDataNodes(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, '/', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS))
        when: 'the new elements are deleted'
            objectUnderTest.deleteDataNode(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, '/invoice[@ProductID ="2"]', OffsetDateTime.now())
        then: 'the original number of datanodes is restored'
            assert originalCountParentlistNodes == countDataNodesInTree(objectUnderTest.getDataNodes(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, '/', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS))
    }

    def 'Add and Delete a (container) data node using #scenario.'() {
        when: 'the new datanode is saved'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , parentXpath, json, now)
        then: 'it can be retrieved by its normalized xpath'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, normalizedXpathToNode, DIRECT_CHILDREN_ONLY)
            assert result.size() == 1
            assert result[0].xpath == normalizedXpathToNode
        and: 'there is now one extra datanode'
            assert originalCountBookstoreChildNodes + 1 == countDataNodesInBookstore()
        when: 'the new datanode is deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, normalizedXpathToNode, now)
        then: 'the original number of data nodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInBookstore()
        where:
            scenario                      | parentXpath                         | json                                                                                        || normalizedXpathToNode
            'normalized parent xpath'     | '/bookstore'                        | '{"webinfo": {"domain-name":"ourbookstore.com", "contact-email":"info@ourbookstore.com" }}' || "/bookstore/webinfo"
            'non-normalized parent xpath' | '/bookstore/categories[ @code="1"]' | '{"books": {"title":"new" }}'                                                               || "/bookstore/categories[@code='1']/books[@title='new']"
    }

    def 'Attempt to create a top level data node using root.'() {
        given: 'a new anchor'
            cpsAdminService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, 'newAnchor1');
        when: 'attempt to save new top level datanode'
            def json = '{"bookstore": {"bookstore-name": "New Store"} }'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, 'newAnchor1' , '/', json, now)
        then: 'since there is no data a data node not found exception is thrown'
            thrown(DataNodeNotFoundException)
    }

    def 'Attempt to save top level data node that already exist'() {
        when: 'attempt to save already existing top level node'
            def json = '{"bookstore": {} }'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, json, now)
        then: 'an exception that (one cps paths is)  already defined is thrown '
            def exceptionThrown = thrown(AlreadyDefinedExceptionBatch)
            exceptionThrown.alreadyDefinedXpaths == [ '/bookstore' ] as Set
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Delete a single datanode with invalid path.'() {
        when: 'attempt to delete a single datanode with invalid path'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/invalid path', now)
        then: 'a cps path parser exception is thrown'
            thrown(CpsPathException)
    }

    def 'Delete multiple data nodes with invalid path.'() {
        when: 'attempt to delete datanode collection with invalid path'
            objectUnderTest.deleteDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, ['/invalid path'], now)
        then: 'the error is silently ignored'
            noExceptionThrown()
    }

    def 'Delete single data node with non-existing path.'() {
        when: 'attempt to delete a single datanode non-existing invalid path'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/does/not/exist', now)
        then: 'a datanode not found exception is thrown'
            thrown(DataNodeNotFoundException)
    }

    def 'Delete multiple data nodes with non-existing path(s).'() {
        when: 'attempt to delete a single datanode non-existing invalid path'
            objectUnderTest.deleteDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, ['/does/not/exist'], now)
        then: 'a  datanode not found (batch) exception is thrown'
            thrown(DataNodeNotFoundExceptionBatch)
    }

    def 'Add and Delete list (element) data nodes.'() {
        given: 'two new (categories) data nodes'
            def json = '{"categories": [ {"code":"new1"}, {"code":"new2" } ] }'
        when: 'the new list elements are saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now)
        then: 'they can be retrieved by their xpaths'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', DIRECT_CHILDREN_ONLY).size() == 1
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', DIRECT_CHILDREN_ONLY).size() == 1
        and: 'there are now two extra data nodes'
            assert originalCountBookstoreChildNodes + 2 == countDataNodesInBookstore()
        when: 'the new elements are deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', now)
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', now)
        then: 'the original number of data nodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInBookstore()
    }

    def 'Add list (element) data nodes that already exist.'() {
        given: 'two (categories) data nodes, one new and one existing'
            def json = '{"categories": [ {"code":"1"}, {"code":"new1"} ] }'
        when: 'attempt to save the list element'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now)
        then: 'an exception that (one cps paths is)  already defined is thrown '
            def exceptionThrown = thrown(AlreadyDefinedExceptionBatch)
            exceptionThrown.alreadyDefinedXpaths == [ '/bookstore/categories[@code=\'1\']' ] as Set
        and: 'there is now one extra data nodes'
            assert originalCountBookstoreChildNodes + 1 == countDataNodesInBookstore()
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Add and Delete list (element) data nodes using lists specific method.'() {
        given: 'a new (categories) data nodes'
            def json = '{"categories": [ {"code":"new1"} ] }'
        and: 'the new list element is saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now)
        when: 'the new element is deleted'
            objectUnderTest.deleteListOrListElement(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', now)
        then: 'the original number of data nodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInBookstore()
    }

    def 'Add and Delete a batch of lists (element) data nodes.'() {
        given: 'two new (categories) data nodes in two separate batches'
            def json1 = '{"categories": [ {"code":"new1"} ] }'
            def json2 = '{"categories": [ {"code":"new2"} ] } '
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.saveListElementsBatch(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', [json1, json2], now)
        then: 'they can be retrieved by their xpaths'
            assert objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', DIRECT_CHILDREN_ONLY).size() == 1
            assert objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', DIRECT_CHILDREN_ONLY).size() == 1
        and: 'there are now two extra data nodes'
            assert originalCountBookstoreChildNodes + 2 == countDataNodesInBookstore()
        when: 'the new elements are deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', now)
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', now)
        then: 'the original number of data nodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInBookstore()
    }

    def 'Add and Delete a batch of lists (element) data nodes with partial success.'() {
        given: 'two new (categories) data nodes in two separate batches'
            def jsonNewElement = '{"categories": [ {"code":"new1"} ] }'
            def jsonExistingElement = '{"categories": [ {"code":"1"} ] } '
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.saveListElementsBatch(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', [jsonNewElement, jsonExistingElement], now)
        then: 'an already defined (batch) exception is thrown for the existing path'
            def exceptionThrown = thrown(AlreadyDefinedExceptionBatch)
            assert exceptionThrown.alreadyDefinedXpaths ==  [ '/bookstore/categories[@code=\'1\']' ] as Set
        and: 'there is now one extra data node'
            assert originalCountBookstoreChildNodes + 1 == countDataNodesInBookstore()
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Attempt to add empty lists.'() {
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.replaceListContent(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', [ ], now)
        then: 'an admin exception is thrown'
            thrown(CpsAdminException)
    }

    def 'Add child error scenario: #scenario.'() {
        when: 'attempt to add a child data node with #scenario'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, parentXpath, json, now)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | parentXpath                              | json                                || expectedException
            'parent does not exist'  | '/bookstore/categories[@code="unknown"]' | '{"books": [ {"title":"new"} ] } '  || DataNodeNotFoundException
            'already existing child' | '/bookstore'                             | '{"categories": [ {"code":"1"} ] }' || AlreadyDefinedExceptionBatch
    }

    def 'Add multiple child data nodes with partial success.'() {
        given: 'one existing and one new list element'
            def json = '{"categories": [ {"code":"1"}, {"code":"new"} ] }'
        when: 'attempt to add the elements'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', json, now)
        then: 'an already defined (batch) exception is thrown for the existing path'
            def thrown  = thrown(AlreadyDefinedExceptionBatch)
            assert thrown.alreadyDefinedXpaths == [ "/bookstore/categories[@code='1']" ] as Set
        and: 'the new data node has been added i.e. can be retrieved'
            assert objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new"]', DIRECT_CHILDREN_ONLY).size() == 1
    }

    def 'Replace list content #scenario.'() {
        given: 'the bookstore categories 1 and 2 exist and have at least 1 child each '
            assert countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="1"]', DIRECT_CHILDREN_ONLY)) > 1
            assert countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="2"]', DIRECT_CHILDREN_ONLY)) > 1
        when: 'the categories list is replaced with just category "1" and without child nodes (books)'
            def json = '{"categories": [ {"code":"' +categoryCode + '"' + childJson + '} ] }'
            objectUnderTest.replaceListContent(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', json, now)
        then: 'the new replaced category can be retrieved but has no children anymore'
            assert expectedNumberOfDataNodes == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="' +categoryCode + '"]', DIRECT_CHILDREN_ONLY))
        when: 'attempt to retrieve a category (code) not in the new list'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="2"]', DIRECT_CHILDREN_ONLY)
        then: 'a datanode not found exception occurs'
            thrown(DataNodeNotFoundException)
        cleanup:
            restoreBookstoreDataAnchor(1)
        where: 'the following data is used'
            scenario                        | categoryCode | childJson                                 || expectedNumberOfDataNodes
            'existing code, no children'    | '1'          | ''                                        || 1
            'existing code, new child'      | '1'          | ', "books" : [ { "title": "New Book" } ]' || 2
            'existing code, existing child' | '1'          | ', "books" : [ { "title": "Matilda" } ]'  || 2
            'new code, new child'           | 'new'        | ', "books" : [ { "title": "New Book" } ]' || 2
    }

    def 'Update multiple data node leaves.'() {
        given: 'Updated json for bookstore data'
            def jsonData =  "{'book-store:books':{'lang':'English/French','price':100,'title':'Matilda'}}"
        when: 'update is performed for leaves'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code='1']", jsonData, now)
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code=1]/books[@title='Matilda']", INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf values are updated as expected'
            assert result.leaves['lang'] == ['English/French']
            assert result.leaves['price'] == [100]
        cleanup:
            restoreBookstoreDataAnchor(2)
    }

    def 'Update data node leaves for node that has no leaves (yet).'() {
        given: 'new (webinfo) datanode without leaves'
            def json = '{"webinfo": {} }'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now)
        when: 'update is performed to add a leaf'
            def updatedJson = '{"webinfo": {"domain-name":"new leaf data"}}'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, "/bookstore", updatedJson, now)
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, "/bookstore/webinfo", INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf value is updated as expected'
            assert result.leaves['domain-name'] == ['new leaf data']
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Update multiple data leaves error scenario: #scenario.'() {
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, xpath, 'irrelevant json data', now)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName               | anchorName                 | xpath           || expectedException
            'invalid dataspace name' | 'Invalid Dataspace'         | 'not-relevant'             | '/not relevant' || DataValidationException
            'invalid anchor name'    | FUNCTIONAL_TEST_DATASPACE_1 | 'INVALID ANCHOR'           | '/not relevant' || DataValidationException
            'non-existing dataspace' | 'non-existing-dataspace'    | 'not-relevant'             | '/not relevant' || DataspaceNotFoundException
            'non-existing anchor'    | FUNCTIONAL_TEST_DATASPACE_1 | 'non-existing-anchor'      | '/not relevant' || AnchorNotFoundException
            'non-existing-xpath'     | FUNCTIONAL_TEST_DATASPACE_1 | BOOKSTORE_ANCHOR_1         | '/non-existing' || DataValidationException
    }

    def 'Update data nodes and descendants.'() {
        given: 'some web info for the bookstore'
            def json = '{"webinfo": {"domain-name":"ourbookstore.com" ,"contact-email":"info@ourbookstore.com" }}'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now)
        when: 'the webinfo (container) is updated'
            json = '{"webinfo": {"domain-name":"newdomain.com" ,"contact-email":"info@newdomain.com" }}'
            objectUnderTest.updateDataNodeAndDescendants(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', json, now)
        then: 'webinfo has been updated with teh new details'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/webinfo', DIRECT_CHILDREN_ONLY)
            result.leaves.'domain-name'[0] == 'newdomain.com'
            result.leaves.'contact-email'[0] == 'info@newdomain.com'
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def countDataNodesInBookstore() {
        return countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', INCLUDE_ALL_DESCENDANTS))
    }

    def countDataNodesInParentlist() {
        return countDataNodesInTree(objectUnderTest.getDataNodes(PARENT_LIST_TEST_DATASPACE, PARENT_LIST_TEST_ANCHOR, '/', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS))
    }
}
