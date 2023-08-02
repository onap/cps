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
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.CpsAdminException
import org.onap.cps.spi.exceptions.CpsPathException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundExceptionBatch
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DeltaReport

import java.time.OffsetDateTime

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataServiceIntegrationSpec extends FunctionalSpecBase {

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
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/', json, now)
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
            def exceptionThrown = thrown(AlreadyDefinedException)
            assert exceptionThrown.alreadyDefinedObjectNames ==  ['/bookstore/categories[@code=\'1\']' ] as Set
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

    def 'Update bookstore top-level container data node.'() {
        when: 'the bookstore top-level container is updated'
            def json = '{ "bookstore": { "bookstore-name": "new bookstore" }}'
            objectUnderTest.updateDataNodeAndDescendants(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/', json, now)
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
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code='1']", jsonData, now)
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2, "/bookstore/categories[@code=1]/books[@title='Matilda']", INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf values are updated as expected'
            assert result.leaves['lang'] == ['English/French']
            assert result.leaves['price'] == [100]
        cleanup:
            restoreBookstoreDataAnchor(2)
    }

    def 'Get delta between 2 anchors for #scenario'() {
        when: 'attempt to get delta report'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_4, xpath, fetchDescendantOption)
        then: 'delta report contains expected number of changes'
            deltaReport.size() == 3
        and: 'delta report contains expected change action'
            assert deltaReport.get(index).getAction() == expectedActions
        and: 'delta report contains expected xpath'
            assert deltaReport.get(index).getXpath() == expectedXpath
        where:
            scenario            | index | xpath || expectedActions || expectedXpath                                            | fetchDescendantOption
            'a node is updated' |   0   | '/'   ||    'update'     || "/bookstore"                                             | OMIT_DESCENDANTS
            'a node is removed' |   1   | '/'   ||    'remove'     || "/bookstore-address[@bookstore-name='Easons-1']"         | OMIT_DESCENDANTS
            'a node is added'   |   2   | '/'   ||     'add'       || "/bookstore-address[@bookstore-name='My New Bookstore']" | OMIT_DESCENDANTS
    }

    def 'Get delta between 2 anchors for nested changes'() {
        def parentNodeXpath = "/bookstore"
        when: 'attempt to get delta report'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_4, parentNodeXpath, INCLUDE_ALL_DESCENDANTS)
        then: 'delta report contains expected number of changes'
            deltaReport.size() == 18
        and: 'the delta report has xpath of parent node'
            def xpaths = getDeltaReportEntities(deltaReport).get('xpaths')
            assert xpaths.contains(parentNodeXpath)
        and: 'the delta report also has expected number of child data nodes'
            xpaths.remove(parentNodeXpath)
            assert xpaths.size() == 17
    }

    def 'Get delta returns empty response when #scenario'() {
        when: 'attempt to get delta report'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, INCLUDE_ALL_DESCENDANTS)
        then: 'delta report is empty'
            assert deltaReport.isEmpty()
        where: 'following data was used'
            scenario                              | sourceAnchor       | targetAnchor       | xpath
        'anchors with identical data are queried' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_5 | '/'
        'same anchor name is passed as parameter' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_3 | '/'
        'non existing xpath'                      | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_4 | '/non-existing-xpath'
    }

    def 'Get delta between anchors error scenario: #scenario'() {
        def xpath = '/not-relevant'
        when: 'attempt to get delta between anchors'
            objectUnderTest.getDeltaByDataspaceAndAnchors(dataspaceName, sourceAnchor, targetAnchor, xpath, INCLUDE_ALL_DESCENDANTS)
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

    def 'Get delta between anchors on addition/deletion of leaves of existing data node scenario: #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, OMIT_DESCENDANTS)
        then: 'expected action is update'
            assert deltaReport.get(0).getAction() == 'update'
        and: 'the payload has expected values'
            Map<String, Serializable> sourceData = deltaReport.get(0).getSourceData() as Map<String, Serializable>
            Map<String, Serializable> targetData = deltaReport.get(0).getTargetData() as Map<String, Serializable>
            assert sourceData.equals(expectedSourceValue)
            assert targetData.equals(expectedTargetValue)
        where: 'following data was used'
            scenario                                | sourceAnchor       | targetAnchor       | xpath                                                     || expectedSourceValue || expectedTargetValue
            'leaf is removed from target data node' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_4 | "/bookstore/categories[@code='5']/books[@title='Book 1']" || [price:1]           || null
            'leaf is added to target data node'     | BOOKSTORE_ANCHOR_4 | BOOKSTORE_ANCHOR_3 | "/bookstore/categories[@code='5']/books[@title='Book 1']" || null                || [price:1]
    }

    def 'Get delta between anchors for delete operation #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_6, BOOKSTORE_ANCHOR_3, parentNodeXpath, INCLUDE_ALL_DESCENDANTS)
        then: 'expected action is update'
            assert deltaReport.get(0).getAction() == 'remove'
        where: 'following data was used'
            scenario                                      | parentNodeXpath
            'source data node has leaves and child nodes' | '/bookstore/categories[@code=\'6\']'
            'source data node has only leaves'            | '/bookstore/categories[@code=\'5\']/books[@title=\'Book 11\']'
            'source data node has child data node only'   | '/bookstore/supportinfo/contact-emails'
            'target data node is empty'                   | '/bookstore/supportinfo'
    }

    def 'Get delta between anchors for update operation, when source data nodes have leaves and child data nodes #scenario'() {
        when: 'attempt to get delta between existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, parentNodeXpath, fetchDescendantsOption)
            def deltaReportEntities = getDeltaReportEntities(deltaReport)
            def xpathsInDeltaReport = deltaReportEntities.get('xpaths')
            def childNodeXpaths = xpathsInDeltaReport.clone() as List
            childNodeXpaths.remove((Object) parentNodeXpath)
        then: 'expected action is update'
            assert deltaReport.get(0).getAction() == 'update'
            assert xpathsInDeltaReport.get(0) == parentNodeXpath
            assert childNodeXpaths.containsAll(expectedChildNodeXpaths)
        and: 'the payload has expected values'
            Map<String, Serializable> sourceData = deltaReport.get(0).getSourceData() as Map<String, Serializable>
            Map<String, Serializable> targetData = deltaReport.get(0).getTargetData() as Map<String, Serializable>
            assert sourceData.equals(expectedSourceData)
            assert targetData.equals(expectedTargetData)
        where: 'following data was used'
            scenario                                             | parentNodeXpath                      | sourceAnchor       | targetAnchor         | fetchDescendantsOption  || expectedSourceData                    || expectedTargetData  || expectedChildNodeXpaths
            'target data nodes have leaves and child data nodes' | '/bookstore/categories[@code=\'1\']' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_6   | INCLUDE_ALL_DESCENDANTS || ['name':'Children']                   || ['name':'Kids']     || ['/bookstore/categories[@code=\'1\']/books[@title=\'The Gruffalo\']', '/bookstore/categories[@code=\'1\']/books[@title=\'Matilda\']']
            'target node has leaves'                             | '/bookstore/categories[@code=\'2\']' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_6   | INCLUDE_ALL_DESCENDANTS || ['name':'Thriller']                   || ['name':'Suspense'] || ['/bookstore/categories[@code=\'2\']/books[@title=\'Annihilation\']']
            'target node has child nodes'                        | '/bookstore'                         | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_6   | DIRECT_CHILDREN_ONLY    || ['bookstore-name':'Easons-1']         || null                || ['/bookstore/supportinfo', '/bookstore/categories[@code=\'2\']', '/bookstore/categories[@code=\'1\']', '/bookstore/categories[@code=\'4\']']
            'target node is empty'                               | '/bookstore'                         | BOOKSTORE_ANCHOR_4 | EMPTY_BOOKSTORE_DATA | DIRECT_CHILDREN_ONLY    || ['bookstore-name':'My New Bookstore'] || null                || ['/bookstore/categories[@code=\'6\']', '/bookstore/categories[@code=\'1\']', '/bookstore/categories[@code=\'5\']', '/bookstore/categories[@code=\'2\']', '/bookstore/container-without-leaves', '/bookstore/categories[@code=\'4\']', '/bookstore/premises', '/bookstore/categories[@code=\'3\']', '/bookstore/supportinfo']
    }

    def 'Get delta between anchors for update operation, when source data node has leaf data only and #scenario'() {
        when: 'attempt to get delta between existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, fetchDescendantsOption)
            def deltaReportEntities = getDeltaReportEntities(deltaReport)
            def xpathsInDeltaReport = deltaReportEntities.get('xpaths')
            def childNodeXpaths = xpathsInDeltaReport.clone() as List
            childNodeXpaths.remove((Object) expectedParentNodeXpath)
        then: 'expected action is update'
            assert deltaReport.get(0).getAction() == 'update'
            assert xpathsInDeltaReport.get(0) == expectedParentNodeXpath
            assert childNodeXpaths.containsAll(expectedChildNodeXpaths)
        and: 'the payload has expected values'
            Map<String, Serializable> sourceData = deltaReport.get(0).getSourceData() as Map<String, Serializable>
            Map<String, Serializable> targetData = deltaReport.get(0).getTargetData() as Map<String, Serializable>
            assert sourceData.equals(expectedSourceData)
            assert targetData.equals(expectedTargetData)
        where: 'following data was used'
            scenario                                      | xpath                            | sourceAnchor       | targetAnchor       | fetchDescendantsOption  || expectedSourceData                 || expectedTargetData  || expectedParentNodeXpath             || expectedChildNodeXpaths
            'target data node has leaves and child nodes' | '/bookstore/categories[@code=2]' | BOOKSTORE_ANCHOR_6 | BOOKSTORE_ANCHOR_3 | DIRECT_CHILDREN_ONLY    || ['name':'Suspense']                || ['name':'Thriller'] || '/bookstore/categories[@code=\'2\']'|| ['/bookstore/categories[@code=\'2\']/books[@title=\'Annihilation\']']
            'target data node has leaves'                 | '/bookstore/categories[@code=1]' | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_6 | OMIT_DESCENDANTS        || ['name':'Children']                || ['name':'Kids']     || '/bookstore/categories[@code=\'1\']'|| []
            'target data node has child data nodes'       | '/bookstore/supportinfo'         | BOOKSTORE_ANCHOR_4 | BOOKSTORE_ANCHOR_6 | INCLUDE_ALL_DESCENDANTS || ['support-office':'test-location'] || null                || '/bookstore/supportinfo'            || ['/bookstore/supportinfo/contact-emails']
            'target data node is empty'                   | '/bookstore'                     | BOOKSTORE_ANCHOR_3 | BOOKSTORE_ANCHOR_6 | OMIT_DESCENDANTS        || ['bookstore-name':'Easons-1']      || null                || '/bookstore'                        || []
    }

    def 'Get delta between anchors for update operation source data node has child data nodes #scenario'() {
        when: 'attempt to get delta between existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, xpath, fetchDescendantsOption)
            def deltaReportEntities = getDeltaReportEntities(deltaReport)
            def xpathsInDeltaReport = deltaReportEntities.get('xpaths')
            def childNodeXpaths = xpathsInDeltaReport.clone() as List
            childNodeXpaths.remove((Object) expectedParentNodeXpath)
        then: 'expected action is update'
            assert deltaReport.get(0).getAction() == 'update'
            assert xpathsInDeltaReport.get(0) == expectedParentNodeXpath
            assert childNodeXpaths.containsAll(expectedChildNodeXpaths)
        and: 'the payload has expected values'
            Map<String, Serializable> sourceData = deltaReport.get(0).getSourceData() as Map<String, Serializable>
            Map<String, Serializable> targetData = deltaReport.get(0).getTargetData() as Map<String, Serializable>
            assert sourceData.equals(expectedSourceData)
            assert targetData.equals(expectedTargetData)
        where: 'following data was used'
            scenario                                           | xpath                    | sourceAnchor         | targetAnchor       | fetchDescendantsOption  || expectedSourceData                      || expectedTargetData                     || expectedParentNodeXpath                                                          || expectedChildNodeXpaths
            'target data node has leaves and child data nodes' | '/bookstore'             | BOOKSTORE_ANCHOR_6   | BOOKSTORE_ANCHOR_3 | DIRECT_CHILDREN_ONLY    || null                                    || ['bookstore-name':'Easons-1']          || '/bookstore'                                                                     || ['/bookstore/supportinfo', '/bookstore/categories[@code=\'2\']', '/bookstore/categories[@code=\'1\']', '/bookstore/categories[@code=\'4\']']
            'target data node has leaves'                      | '/bookstore/supportinfo' | BOOKSTORE_ANCHOR_6   | BOOKSTORE_ANCHOR_4 | INCLUDE_ALL_DESCENDANTS || null                                    || ['support-office':'test-location']     || '/bookstore/supportinfo'                                                         || ['/bookstore/supportinfo/contact-emails']
            'target data node has child data nodes'            | '/bookstore/premises'    | BOOKSTORE_ANCHOR_3   | BOOKSTORE_ANCHOR_6 | INCLUDE_ALL_DESCENDANTS || ['town':'Maynooth', 'county':'Kildare'] || ['town':'Killarney', 'county':'Kerry'] || '/bookstore/premises/addresses[@house-number=\'2\' and @street=\'Main Street\']' || []
    }

    def 'Get delta between anchors for update operation when soource data node is empty #scenario'() {
        when: 'attempt to get delta between existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, '/bookstore', fetchDescendantsOption)
            def deltaReportEntities = getDeltaReportEntities(deltaReport)
            def xpathsInDeltaReport = deltaReportEntities.get('xpaths')
            def childNodeXpaths = xpathsInDeltaReport.clone() as List
            childNodeXpaths.remove((Object) '/bookstore')
        then: 'expected action is update'
            assert deltaReport.get(0).getAction() == 'update'
            assert xpathsInDeltaReport.get(0) == '/bookstore'
            assert childNodeXpaths.containsAll(expectedChildNodeXpaths)
        and: 'the payload has expected values'
            Map<String, Serializable> sourceData = deltaReport.get(0).getSourceData() as Map<String, Serializable>
            Map<String, Serializable> targetData = deltaReport.get(0).getTargetData() as Map<String, Serializable>
            assert sourceData.equals(null)
            assert targetData.equals(expectedTargetData)
        where: 'following data was used'
            scenario                                          | sourceAnchor         | targetAnchor       | fetchDescendantsOption || expectedTargetData                    || expectedChildNodeXpaths
            'target data node has leaves and child data node' | EMPTY_BOOKSTORE_DATA | BOOKSTORE_ANCHOR_4 | DIRECT_CHILDREN_ONLY   || ['bookstore-name':'My New Bookstore'] || ['/bookstore/categories[@code=\'6\']', '/bookstore/categories[@code=\'1\']', '/bookstore/categories[@code=\'5\']', '/bookstore/categories[@code=\'2\']', '/bookstore/container-without-leaves', '/bookstore/categories[@code=\'4\']', '/bookstore/premises', '/bookstore/categories[@code=\'3\']', '/bookstore/supportinfo']
            'target data node has leaves'                     | BOOKSTORE_ANCHOR_6   | BOOKSTORE_ANCHOR_3 | OMIT_DESCENDANTS       || ['bookstore-name':'Easons-1']         || []
    }

    def 'Get delta between anchors for update operation edge case #scenario'() {
        given: 'parent node xpath and expected child node xpaths of delta report'
            def parentNodeXpath = '/bookstore'
            def expectedChildNodeXpathsInDelta = ['/bookstore/premises', '/bookstore/categories[@code=\'3\']', '/bookstore/supportinfo', '/bookstore/categories[@code=\'2\']', '/bookstore/categories[@code=\'1\']', '/bookstore/categories[@code=\'5\']']
        when: 'attempt to get delta between leaves of existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, sourceAnchor, targetAnchor, parentNodeXpath, DIRECT_CHILDREN_ONLY)
            def deltaReportEntities = getDeltaReportEntities(deltaReport)
            def xpathsInDeltaReport = deltaReportEntities.get('xpaths')
        then: 'expected child node xpaths are present in delta report'
            assert !xpathsInDeltaReport.contains(parentNodeXpath)
            assert xpathsInDeltaReport.containsAll(expectedChildNodeXpathsInDelta)
        where: 'the following data is used'
            scenario                                                           | sourceAnchor         | targetAnchor
            'source data node has child data nodes, target data node is empty' | BOOKSTORE_ANCHOR_6   | EMPTY_BOOKSTORE_DATA
            'source data node is empty, target data node has child data node'  | EMPTY_BOOKSTORE_DATA | BOOKSTORE_ANCHOR_6
    }

    def "Delta report for no update scenario" () {
        when: 'attempt to get delta between data nodes with no leaf data'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_4, BOOKSTORE_ANCHOR_6, "/bookstore/container-without-leaves", INCLUDE_ALL_DESCENDANTS)
        then: 'the expected action is present in delta report'
            deltaReport.size() == 0
    }

    def 'Get delta between anchors for added nodes #scenario'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, EMPTY_BOOKSTORE_DATA, BOOKSTORE_ANCHOR_4, "/bookstore", INCLUDE_ALL_DESCENDANTS)
        then: 'the expected action is present in delta report'
            deltaReport.get(index).getAction() == 'add'
        and: 'the expected payload is present in delta report'
            deltaReport.get(index).getXpath() == expectedChildNodeXpath
        where: 'following data was used'
            scenario                                     | index || expectedChildNodeXpath
            'node with only leaves'                      |   1   || "/bookstore/categories[@code='6']"
            'node with leaves and child data node'       |   2   || "/bookstore/categories[@code='1']"
            'node with no leaves and no child data node' |   19  || "/bookstore/container-without-leaves"
            'node with only child data node'             |   25  || "/bookstore/premises/addresses[@house-number='2' and @street='Main Street']"
    }

    def 'Get delta between anchors edge case: added nodes when source data nodes are empty'() {
        when: 'attempt to get delta between leaves of existing data nodes'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_ANCHOR_3, BOOKSTORE_ANCHOR_4, "/bookstore/categories[@code='6']", INCLUDE_ALL_DESCENDANTS)
        then: 'the expected action is present in delta report'
            deltaReport.get(0).getAction() == 'add'
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
