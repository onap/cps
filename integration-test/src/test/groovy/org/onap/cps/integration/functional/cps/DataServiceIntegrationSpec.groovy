/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023-2024 TechMahindra Ltd.
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

package org.onap.cps.integration.functional.cps

import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.CpsAdminException
import org.onap.cps.spi.exceptions.CpsPathException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DeltaReport
import org.onap.cps.utils.ContentType

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class DataServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataService objectUnderTest
    def originalCountBookstoreChildNodes
    def originalCountBookstoreTopLevelListNodes

    def setup() {
        objectUnderTest = cpsDataService
        originalCountBookstoreChildNodes = countDataNodesInBookstore()
        originalCountBookstoreTopLevelListNodes = countTopLevelListDataNodesInBookstore()
    }

    def 'Read bookstore top-level container(s) using #fetchDescendantsOption.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', fetchDescendantsOption)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert countDataNodesInTree(result) == expectNumberOfDataNodes
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons-1']
        and: 'they are from the correct dataspace'
            assert result.dataspace == [FUNCTIONAL_TEST_DATASPACE_1]
        and: 'they are from the correct anchor'
            assert result.anchorName == [BOOKSTORE_ANCHOR_1]
        where: 'the following option is used'
            fetchDescendantsOption        || expectNumberOfDataNodes
            OMIT_DESCENDANTS              || 1
            DIRECT_CHILDREN_ONLY          || 7
            INCLUDE_ALL_DESCENDANTS       || 28
            new FetchDescendantsOption(2) || 28
    }

    def 'Read bookstore top-level container(s) using "root" path variations.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, root, OMIT_DESCENDANTS)
        then: 'the tree consist correct number of data nodes'
            assert countDataNodesInTree(result) == 2
        and: 'the top level data node has the expected number of leaves'
            assert result.leaves.size() == 2
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

    def 'Get data nodes error scenario #scenario'() {
        when: 'attempt to retrieve data nodes'
            objectUnderTest.getDataNodes(dataspaceName, anchorName, xpath, OMIT_DESCENDANTS)
        then: 'expected exception is thrown'
            thrown(expectedException)
        where: 'following data is used'
            scenario                 | dataspaceName                | anchorName        | xpath           || expectedException
            'non existent dataspace' | 'non-existent'               | 'not-relevant'    | '/not-relevant' || DataspaceNotFoundException
            'non existent anchor'    | FUNCTIONAL_TEST_DATASPACE_1  | 'non-existent'    | '/not-relevant' || AnchorNotFoundException
            'non-existent xpath'     | FUNCTIONAL_TEST_DATASPACE_1  | BOOKSTORE_ANCHOR_1| '/non-existing' || DataNodeNotFoundException
            'invalid-dataspace'      | 'Invalid dataspace'          | 'not-relevant'    | '/not-relevant' || DataValidationException
            'invalid-dataspace'      | FUNCTIONAL_TEST_DATASPACE_1  | 'Invalid Anchor'  | '/not-relevant' || DataValidationException
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

    def 'Get whole list data' () {
            def xpathForWholeList = "/bookstore/categories"
        when: 'get data nodes for bookstore container'
            def dataNodes = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, xpathForWholeList, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert dataNodes.size() == 5
        and: 'each datanode contains the list node xpath partially in its xpath'
            dataNodes.each {dataNode ->
                assert dataNode.xpath.contains(xpathForWholeList)
            }
    }

    def 'Read (multiple) data nodes with #scenario' () {
        when: 'attempt to get data nodes using multiple valid xpaths'
            def dataNodes = objectUnderTest.getDataNodesForMultipleXpaths(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, xpath, OMIT_DESCENDANTS)
        then: 'expected numer of data nodes are returned'
            dataNodes.size() == expectedNumberOfDataNodes
        where: 'the following data was used'
                    scenario                    |                       xpath                                       |   expectedNumberOfDataNodes
            'container-node xpath'              | ['/bookstore']                                                    |               1
            'list-item'                         | ['/bookstore/categories[@code=1]']                                |               1
            'parent-list xpath'                 | ['/bookstore/categories']                                         |               5
            'child-list xpath'                  | ['/bookstore/categories[@code=1]/books']                          |               2
            'both parent and child list xpath'  | ['/bookstore/categories', '/bookstore/categories[@code=1]/books'] |               7
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
            cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, 'newAnchor1');
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
            def exceptionThrown = thrown(AlreadyDefinedException)
            exceptionThrown.alreadyDefinedObjectNames == ['/bookstore' ] as Set
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

    def 'Add and Delete top-level list (element) data nodes with root node.'() {
        given: 'a new (multiple-data-tree:invoice) datanodes'
            def json = '{"bookstore-address":[{"bookstore-name":"Easons","address":"Bangalore,India","postal-code":"560043"}]}'
        when: 'the new list elements are saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/', json, now, ContentType.JSON)
        then: 'they can be retrieved by their xpaths'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore-address[@bookstore-name="Easons"]', INCLUDE_ALL_DESCENDANTS)
        and: 'there is one extra datanode'
            assert originalCountBookstoreTopLevelListNodes + 1 == countTopLevelListDataNodesInBookstore()
        when: 'the new elements are deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore-address[@bookstore-name="Easons"]', now)
        then: 'the original number of datanodes is restored'
            assert originalCountBookstoreTopLevelListNodes == countTopLevelListDataNodesInBookstore()
    }

    def 'Add and Delete list (element) data nodes.'() {
        given: 'two new (categories) data nodes'
            def json = '{"categories": [ {"code":"new1"}, {"code":"new2" } ] }'
        when: 'the new list elements are saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now, ContentType.JSON)
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
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now, ContentType.JSON)
        then: 'an exception that (one cps paths is)  already defined is thrown '
            def exceptionThrown = thrown(AlreadyDefinedException)
            exceptionThrown.alreadyDefinedObjectNames == ['/bookstore/categories[@code=\'1\']' ] as Set
        and: 'there is now one extra data nodes'
            assert originalCountBookstoreChildNodes + 1 == countDataNodesInBookstore()
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Add and Delete list (element) data nodes using lists specific method.'() {
        given: 'a new (categories) data nodes'
            def json = '{"categories": [ {"code":"new1"} ] }'
        and: 'the new list element is saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now, ContentType.JSON)
        when: 'the new element is deleted'
            objectUnderTest.deleteListOrListElement(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', now)
        then: 'the original number of data nodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInBookstore()
    }

    def 'Add and Delete a batch of list element data nodes.'() {
        given: 'two new (categories) data nodes in a single batch'
            def json = '{"categories": [ {"code":"new1"}, {"code":"new2"} ] }'
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now, ContentType.JSON)
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

    def 'Add and Delete a batch of list element data nodes with partial success.'() {
        given: 'one existing and one new (categories) data nodes in a single batch'
            def json = '{"categories": [ {"code":"new1"}, {"code":"1"} ] }'
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now, ContentType.JSON)
        then: 'an already defined (batch) exception is thrown for the existing path'
            def exceptionThrown = thrown(AlreadyDefinedException)
            assert exceptionThrown.alreadyDefinedObjectNames ==  ['/bookstore/categories[@code=\'1\']' ] as Set
        and: 'there is now one extra data node'
            assert originalCountBookstoreChildNodes + 1 == countDataNodesInBookstore()
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Attempt to add empty lists.'() {
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.replaceListContent(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', [ ] as String, now, ContentType.JSON)
        then: 'an dataValidation exception is thrown'
            thrown(DataValidationException)
    }

    def 'Add child error scenario: #scenario.'() {
        when: 'attempt to add a child data node with #scenario'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, parentXpath, json, now)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | parentXpath                              | json                                || expectedException
            'parent does not exist'  | '/bookstore/categories[@code="unknown"]' | '{"books": [ {"title":"new"} ] } '  || DataNodeNotFoundException
            'already existing child' | '/bookstore'                             | '{"categories": [ {"code":"1"} ] }' || AlreadyDefinedException
    }

    def 'Add multiple child data nodes with partial success.'() {
        given: 'one existing and one new list element'
            def json = '{"categories": [ {"code":"1"}, {"code":"new"} ] }'
        when: 'attempt to add the elements'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', json, now)
        then: 'an already defined (batch) exception is thrown for the existing path'
            def thrown  = thrown(AlreadyDefinedException)
            assert thrown.alreadyDefinedObjectNames == [ "/bookstore/categories[@code='1']" ] as Set
        and: 'the new data node has been added i.e. can be retrieved'
            assert objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new"]', DIRECT_CHILDREN_ONLY).size() == 1
    }

    def 'Replace list content #scenario.'() {
        given: 'the bookstore categories 1 and 2 exist and have at least 1 child each '
            assert countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="1"]', DIRECT_CHILDREN_ONLY)) > 1
            assert countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="2"]', DIRECT_CHILDREN_ONLY)) > 1
        when: 'the categories list is replaced with just category "1" and without child nodes (books)'
            def json = '{"categories": [ {"code":"' +categoryCode + '"' + childJson + '} ] }'
            objectUnderTest.replaceListContent(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', json, now, ContentType.JSON)
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

    def 'Update data node leaves for node that has no leaves (yet).'() {
        given: 'new (webinfo) datanode without leaves'
            def json = '{"webinfo": {} }'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, now)
        when: 'update is performed to add a leaf'
            def updatedJson = '{"webinfo": {"domain-name":"new leaf data"}}'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, "/bookstore", updatedJson, now, ContentType.JSON)
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, "/bookstore/webinfo", INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf value is updated as expected'
            assert result.leaves['domain-name'] == ['new leaf data']
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Update multiple data leaves error scenario: #scenario.'() {
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, xpath, 'irrelevant json data', now, ContentType.JSON)
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
            objectUnderTest.updateDataNodeAndDescendants(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', json, now, ContentType.JSON)
        then: 'webinfo has been updated with teh new details'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/webinfo', DIRECT_CHILDREN_ONLY)
            result.leaves.'domain-name'[0] == 'newdomain.com'
            result.leaves.'contact-email'[0] == 'info@newdomain.com'
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Update bookstore top-level container data node.'() {
        when: 'the bookstore top-level container is updated'
            def json = '{ "bookstore": { "bookstore-name": "new bookstore" }}'
            objectUnderTest.updateDataNodeAndDescendants(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/', json, now, ContentType.JSON)
        then: 'bookstore name has been updated'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', DIRECT_CHILDREN_ONLY)
            result.leaves.'bookstore-name'[0] == 'new bookstore'
        cleanup:
            restoreBookstoreDataAnchor(1)
    }

    def 'Update multiple data node leaves.'() {
        given: 'Updated json for bookstore data'
            def jsonData =  "{'book-store:books':{'lang':'English/French','price':100,'title':'Matilda'}}"
        when: 'update is performed for leaves'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code='1']", jsonData, now, ContentType.JSON)
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code=1]/books[@title='Matilda']", INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf values are updated as expected'
            assert result[0].leaves['lang'] == 'English/French'
            assert result[0].leaves['price'] == 100
        cleanup:
            restoreBookstoreDataAnchor(2)
    }

    def 'Order of leaf-list elements is preserved when "ordered-by user" is set in the YANG model.'() {
        given: 'Updated json for bookstore data'
            def jsonData =  "{'book-store:books':{'title':'Matilda', 'authors': ['beta', 'alpha', 'gamma', 'delta']}}"
        when: 'update is performed for leaves'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code='1']", jsonData, now, ContentType.JSON)
        and: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code=1]/books[@title='Matilda']", INCLUDE_ALL_DESCENDANTS)
        then: 'the leaf-list values have expected order'
            assert result[0].leaves['authors'] == ['beta', 'alpha', 'gamma', 'delta']
        cleanup:
            restoreBookstoreDataAnchor(2)
    }

    def 'Leaf-list elements are sorted when "ordered-by user" is not set in the YANG model.'() {
        given: 'Updated json for bookstore data'
            def jsonData =  "{'book-store:books':{'title':'Matilda', 'editions': [2011, 1988, 2001, 2022, 2025]}}"
        when: 'update is performed for leaves'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code='1']", jsonData, now, ContentType.JSON)
        and: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code=1]/books[@title='Matilda']", INCLUDE_ALL_DESCENDANTS)
        then: 'the leaf-list values have natural order'
            assert result[0].leaves['editions'] == [1988, 2001, 2011, 2022, 2025]
        cleanup:
            restoreBookstoreDataAnchor(2)
    }

    def 'Get delta between 2 anchors'() {
        when: 'attempt to get delta report between anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_5, '/', OMIT_DESCENDANTS)
        and: 'report is ordered based on xpath'
            result = result.toList().sort { it.xpath }
        then: 'delta report contains expected number of changes'
            result.size() == 3
        and: 'delta report contains REPLACE action with expected xpath'
            assert result[0].getAction() == 'replace'
            assert result[0].getXpath() == '/bookstore'
        and: 'delta report contains CREATE action with expected xpath'
            assert result[1].getAction() == 'create'
            assert result[1].getXpath() == "/bookstore-address[@bookstore-name='Crossword Bookstores']"
        and: 'delta report contains REMOVE action with expected xpath'
            assert result[2].getAction() == 'remove'
            assert result[2].getXpath() == "/bookstore-address[@bookstore-name='Easons-1']"
    }

    def 'Get delta between 2 anchors returns empty response when #scenario'() {
        when: 'attempt to get delta report between anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, targetAnchor, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'delta report is empty'
            assert result.isEmpty()
        where: 'following data was used'
            scenario                              | targetAnchor       | xpath
        'anchors with identical data are queried' | BOOKSTORE_ANCHOR_4 | '/'
        'same anchor name is passed as parameter' | BOOKSTORE_ANCHOR_3 | '/'
        'non existing xpath'                      | BOOKSTORE_ANCHOR_5 | '/non-existing-xpath'
    }

    def 'Get delta between anchors error scenario: #scenario'() {
        when: 'attempt to get delta between anchors'
            objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, sourceAnchor, targetAnchor, '/some-xpath', INCLUDE_ALL_DESCENDANTS)
        then: 'expected exception is thrown'
            thrown(expectedException)
        where: 'following data was used'
                    scenario                               | dataspaceName               | sourceAnchor          | targetAnchor          || expectedException
            'invalid dataspace name'                       | 'Invalid dataspace'         | 'not-relevant'        | 'not-relevant'        || DataValidationException
            'invalid anchor 1 name'                        | FUNCTIONAL_TEST_DATASPACE_3 | 'invalid anchor'      | 'not-relevant'        || DataValidationException
            'invalid anchor 2 name'                        | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | 'invalid anchor'      || DataValidationException
            'non-existing dataspace'                       | 'non-existing'              | 'not-relevant1'       | 'not-relevant2'       || DataspaceNotFoundException
            'non-existing dataspace with same anchor name' | 'non-existing'              | 'not-relevant'        | 'not-relevant'        || DataspaceNotFoundException
            'non-existing anchor 1'                        | FUNCTIONAL_TEST_DATASPACE_3 | 'non-existing-anchor' | 'not-relevant'        || AnchorNotFoundException
            'non-existing anchor 2'                        | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | 'non-existing-anchor' || AnchorNotFoundException
    }

    def 'Get delta between anchors for remove action, where source data node #scenario'() {
        when: 'attempt to get delta between leaves of data nodes present in 2 anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_5, BOOKSTORE_ANCHOR_3, parentNodeXpath, INCLUDE_ALL_DESCENDANTS)
        then: 'expected action is present in delta report'
            assert result.get(0).getAction() == 'remove'
        where: 'following data was used'
            scenario                     | parentNodeXpath
            'has leaves and child nodes' | "/bookstore/categories[@code='6']"
            'has leaves only'            | "/bookstore/categories[@code='5']/books[@title='Book 11']"
            'has child data node only'   | "/bookstore/support-info/contact-emails"
            'is empty'                   | "/bookstore/container-without-leaves"
    }

    def 'Get delta between anchors for "create" action, where target data node #scenario'() {
        when: 'attempt to get delta between leaves of data nodes present in 2 anchors'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_5, parentNodeXpath, INCLUDE_ALL_DESCENDANTS)
        then: 'the expected action is present in delta report'
            result.get(0).getAction() == 'create'
        and: 'the expected xapth is present in delta report'
            result.get(0).getXpath() == parentNodeXpath
        where: 'following data was used'
            scenario                     | parentNodeXpath
            'has leaves and child nodes' | "/bookstore/categories[@code='6']"
            'has leaves only'            | "/bookstore/categories[@code='5']/books[@title='Book 11']"
            'has child data node only'   | "/bookstore/support-info/contact-emails"
            'is empty'                   | "/bookstore/container-without-leaves"
    }

    def 'Get delta between anchors when leaves of existing data nodes are updated,: #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, OMIT_DESCENDANTS)
        then: 'expected action is "replace"'
            assert result[0].getAction() == 'replace'
        and: 'the payload has expected leaf values'
            def sourceData = result[0].getSourceData()
            def targetData = result[0].getTargetData()
            assert sourceData == expectedSourceValue
            assert targetData == expectedTargetValue
        where: 'following data was used'
            scenario                           | sourceAnchor       | targetAnchor       | xpath                                                     || expectedSourceValue            | expectedTargetValue
            'leaf is updated in target anchor' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore'                                              || ['bookstore-name': 'Easons-1'] | ['bookstore-name': 'Crossword Bookstores']
            'leaf is removed in target anchor' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | "/bookstore/categories[@code='5']/books[@title='Book 1']" || [price:1]                      | null
            'leaf is added in target anchor'   | BOOKSTORE_ANCHOR_5 | BOOKSTORE_ANCHOR_3 | "/bookstore/categories[@code='5']/books[@title='Book 1']" || null                           | [price:1]
    }

    def 'Get delta between anchors when child data nodes under existing parent data nodes are updated: #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, DIRECT_CHILDREN_ONLY)
        then: 'expected action is "replace"'
            assert result[0].getAction() == 'replace'
        and: 'the delta report has expected child node xpaths'
            def deltaReportEntities = getDeltaReportEntities(result)
            def childNodeXpathsInDeltaReport = deltaReportEntities.get('xpaths')
            assert childNodeXpathsInDeltaReport.contains(expectedChildNodeXpath)
        where: 'following data was used'
            scenario                                          | sourceAnchor       | targetAnchor       | xpath                 || expectedChildNodeXpath
            'source and target anchors have child data nodes' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore/premises' || '/bookstore/premises/addresses[@house-number=\'2\' and @street=\'Main Street\']'
            'removed child data nodes in target anchor'       | BOOKSTORE_ANCHOR_5 | BOOKSTORE_ANCHOR_3 | '/bookstore'          || '/bookstore/support-info'
            'added  child data nodes in target anchor'        | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/bookstore'          || '/bookstore/support-info'
    }

    def 'Get delta between anchors where source and target data nodes have leaves and child data nodes'() {
        given: 'parent node xpath and expected data in delta report'
            def parentNodeXpath = "/bookstore/categories[@code='1']"
            def expectedSourceDataInParentNode = ['name':'Children']
            def expectedTargetDataInParentNode = ['name':'Kids']
            def expectedSourceDataInChildNode = [['lang' : 'English'],['price':20, 'editions':[1988, 2000]]]
            def expectedTargetDataInChildNode = [['lang':'English/German'], ['price':200, 'editions':[1988, 2000, 2023]]]
        when: 'attempt to get delta between leaves of existing data nodes'
            def result = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_5, parentNodeXpath, INCLUDE_ALL_DESCENDANTS)
            def deltaReportEntities = getDeltaReportEntities(result)
        then: 'expected action is "replace"'
            assert result[0].getAction() == 'replace'
        and: 'the payload has expected parent node xpath'
            assert deltaReportEntities.get('xpaths').contains(parentNodeXpath)
        and: 'delta report has expected source and target data'
            assert deltaReportEntities.get('sourcePayload').contains(expectedSourceDataInParentNode)
            assert deltaReportEntities.get('targetPayload').contains(expectedTargetDataInParentNode)
        and: 'the delta report also has expected child node xpaths'
            assert deltaReportEntities.get('xpaths').containsAll(["/bookstore/categories[@code='1']/books[@title='The Gruffalo']", "/bookstore/categories[@code='1']/books[@title='Matilda']"])
        and: 'the delta report also has expected source and target data of child nodes'
            assert deltaReportEntities.get('sourcePayload').containsAll(expectedSourceDataInChildNode)
            assert deltaReportEntities.get('targetPayload').containsAll(expectedTargetDataInChildNode)
    }

    def 'Get delta between anchor and JSON payload'() {
        when: 'attempt to get delta report between anchor and JSON payload'
            def jsonPayload = "{\"book-store:bookstore\":{\"bookstore-name\":\"Crossword Bookstores\"},\"book-store:bookstore-address\":{\"address\":\"Bangalore, India\",\"postal-code\":\"560062\",\"bookstore-name\":\"Crossword Bookstores\"}}"
            def result = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, '/', [:], jsonPayload, OMIT_DESCENDANTS)
        then: 'delta report contains expected number of changes'
            result.size() == 3
        and: 'delta report contains "replace" action with expected xpath'
            assert result[0].getAction() == 'replace'
            assert result[0].getXpath() == '/bookstore'
        and: 'delta report contains "remove" action with expected xpath'
            assert result[1].getAction() == 'remove'
            assert result[1].getXpath() == "/bookstore-address[@bookstore-name='Easons-1']"
        and: 'delta report contains "create" action with expected xpath'
            assert result[2].getAction() == 'create'
            assert result[2].getXpath() == "/bookstore-address[@bookstore-name='Crossword Bookstores']"
    }

    def 'Get delta between anchor and payload returns empty response when JSON payload is identical to anchor data'() {
        when: 'attempt to get delta report between anchor and JSON payload (replacing the string Easons with Easons-1 because the data in JSON file is modified, to append anchor number, during the setup process of the integration tests)'
            def jsonPayload = readResourceDataFile('bookstore/bookstoreData.json').replace('Easons', 'Easons-1')
            def result = objectUnderTest.getDeltaByDataspaceAnchorAndPayload(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, '/', [:], jsonPayload, INCLUDE_ALL_DESCENDANTS)
        then: 'delta report is empty'
            assert result.isEmpty()
    }

    def 'Get delta between anchor and payload error scenario: #scenario'() {
        when: 'attempt to get delta between anchor and json payload'
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(dataspaceName, sourceAnchor, xpath, [:], jsonPayload, INCLUDE_ALL_DESCENDANTS)
        then: 'expected exception is thrown'
            thrown(expectedException)
        where: 'following data was used'
                scenario                               | dataspaceName               | sourceAnchor          | xpath        | jsonPayload   || expectedException
        'invalid dataspace name'                       | 'Invalid dataspace'         | 'not-relevant'        | '/'          | '{some-json}' || DataValidationException
        'invalid anchor name'                          | FUNCTIONAL_TEST_DATASPACE_3 | 'invalid anchor'      | '/'          | '{some-json}' || DataValidationException
        'non-existing dataspace'                       | 'non-existing'              | 'not-relevant'        | '/'          | '{some-json}' || DataspaceNotFoundException
        'non-existing anchor'                          | FUNCTIONAL_TEST_DATASPACE_3 | 'non-existing-anchor' | '/'          | '{some-json}' || AnchorNotFoundException
        'empty json payload with root node xpath'      | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | '/'          | ''            || DataValidationException
        'empty json payload with non-root node xpath'  | FUNCTIONAL_TEST_DATASPACE_3 | BOOKSTORE_ANCHOR_3    | '/bookstore' | ''            || DataValidationException
    }

    def getDeltaReportEntities(List<DeltaReport> deltaReport) {
        def xpaths = []
        def action = []
        def sourcePayload = []
        def targetPayload = []
        deltaReport.each {
            delta -> xpaths.add(delta.getXpath())
                action.add(delta.getAction())
                sourcePayload.add(delta.getSourceData())
                targetPayload.add(delta.getTargetData())
        }
        return ['xpaths':xpaths, 'action':action, 'sourcePayload':sourcePayload, 'targetPayload':targetPayload]
    }

    def countDataNodesInBookstore() {
        return countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', INCLUDE_ALL_DESCENDANTS))
    }

    def countTopLevelListDataNodesInBookstore() {
        return countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/', INCLUDE_ALL_DESCENDANTS))
    }
}
