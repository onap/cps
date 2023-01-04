/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.utils

import org.onap.cps.TestUtils
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

class YangUtilsSpec extends Specification {
    def 'Parsing a valid multicontainer Json String.'() {
        given: 'a yang model (file)'
            def jsonData = org.onap.cps.TestUtils.getResourceFileContent('multiple-object-data.json')
        and: 'a model for that data'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('multipleDataTree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the json data is parsed'
            def result = YangUtils.parseJsonData(jsonData, schemaContext)
        then: 'a ContainerNode holding collection of normalized nodes is returned'
            result.body().getAt(index) instanceof NormalizedNode == true
        then: 'qualified name of children created is as expected'
            result.body().getAt(index).getIdentifier().nodeType == QName.create('org:onap:ccsdk:multiDataTree', '2020-09-15', nodeName)
        where:
            index   | nodeName
            0       | 'first-container'
            1       | 'last-container'
    }

    def 'Parsing a valid #scenario String.'() {
        given: 'a yang model (file)'
            def fileData = org.onap.cps.TestUtils.getResourceFileContent(contentFile)
        and: 'a model for that data'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the data is parsed'
            NormalizedNode result = YangUtils.parseData(contentType, fileData, schemaContext)
        then: 'the result is a normalized node of the correct type'
            if (revision) {
                result.identifier.nodeType == QName.create(namespace, revision, localName)
            } else {
                result.identifier.nodeType == QName.create(namespace, localName)
            }
        where:
            scenario | contentFile      | contentType      | namespace                                 | revision     | localName
            'JSON'   | 'bookstore.json' | ContentType.JSON | 'org:onap:ccsdk:sample'                   | '2020-09-15' | 'bookstore'
            'XML'    | 'bookstore.xml'  | ContentType.XML  | 'urn:ietf:params:xml:ns:netconf:base:1.0' | ''           | 'bookstore'
    }

    def 'Parsing invalid data: #description.'() {
        given: 'a yang model (file)'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'invalid data is parsed'
            YangUtils.parseData(contentType, invalidData, schemaContext)
        then: 'an exception is thrown'
            thrown(DataValidationException)
        where: 'the following invalid data is provided'
            invalidData                                                                          | contentType      | description
            '{incomplete json'                                                                   | ContentType.JSON | 'incomplete json'
            '{"test:bookstore": {"address": "Parnell st." }}'                                    | ContentType.JSON | 'json with un-modelled data'
            '{" }'                                                                               | ContentType.JSON | 'json with syntax exception'
            '<data>'                                                                             | ContentType.XML  | 'incomplete xml'
            '<data><bookstore><bookstore-anything>blabla</bookstore-anything></bookstore</data>' | ContentType.XML  | 'xml with invalid model'
            ''                                                                                   | ContentType.XML  | 'empty xml'
    }

    def 'Parsing data fragment by xpath for #scenario.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'json string is parsed'
            def result = YangUtils.parseData(contentType, nodeData, schemaContext, parentNodeXpath)
        then: 'a ContainerNode holding collection of normalized nodes is returned'
            result.body().getAt(0) instanceof NormalizedNode == true
        then: 'result represents a node of expected type'
            result.body().getAt(0).getIdentifier().nodeType == QName.create('org:onap:cps:test:test-tree', '2020-02-02', nodeName)
        where:
            scenario                         | contentType      | nodeData                                                                                                                                                                                                      | parentNodeXpath                       || nodeName
            'JSON list element as container' | ContentType.JSON | '{ "branch": { "name": "B", "nest": { "name": "N", "birds": ["bird"] } } }'                                                                                                                                   | '/test-tree'                          || 'branch'
            'JSON list element within list'  | ContentType.JSON | '{ "branch": [{ "name": "B", "nest": { "name": "N", "birds": ["bird"] } }] }'                                                                                                                                 | '/test-tree'                          || 'branch'
            'JSON container element'         | ContentType.JSON | '{ "nest": { "name": "N", "birds": ["bird"] } }'                                                                                                                                                              | '/test-tree/branch[@name=\'Branch\']' || 'nest'
            'XML element test tree'          | ContentType.XML  | '<?xml version=\'1.0\' encoding=\'UTF-8\'?><branch xmlns="org:onap:cps:test:test-tree"><name>Left</name><nest><name>Small</name><birds>Sparrow</birds></nest></branch>'                                       | '/test-tree'                          || 'branch'
            'XML element branch xpath'       | ContentType.XML  | '<?xml version=\'1.0\' encoding=\'UTF-8\'?><branch xmlns="org:onap:cps:test:test-tree"><name>Left</name><nest><name>Small</name><birds>Sparrow</birds><birds>Robin</birds></nest></branch>'                   | '/test-tree'                          || 'branch'
            'XML container element'          | ContentType.XML  | '<?xml version=\'1.0\' encoding=\'UTF-8\'?><nest xmlns="org:onap:cps:test:test-tree"><name>Small</name><birds>Sparrow</birds></nest>'                                                                         | '/test-tree/branch[@name=\'Branch\']' || 'nest'
    }

    def 'Parsing json data fragment by xpath error scenario: #scenario.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'json string is parsed'
            YangUtils.parseJsonData('{"nest": {"name" : "Nest", "birds": ["bird"]}}', schemaContext,
                    parentNodeXpath)
        then: 'expected exception is thrown'
            thrown(DataValidationException)
        where:
            scenario                             | parentNodeXpath
            'xpath has no identifiers'           | '/'
            'xpath has no valid identifiers'     | '/[@name=\'Name\']'
            'invalid parent path'                | '/test-bush'
            'another invalid parent path'        | '/test-tree/branch[@name=\'Branch\']/nest/name/last-name'
            'fragment does not belong to parent' | '/test-tree/'
    }

    def 'Parsing json data with invalid json string: #description.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'malformed json string is parsed'
            YangUtils.parseJsonData(invalidJson, schemaContext)
        then: 'an exception is thrown'
            thrown(DataValidationException)
        where: 'the following malformed json is provided'
            description                                          | invalidJson
            'malformed json string with unterminated array data' | '{bookstore={categories=[{books=[{authors=[Iain M. Banks]}]}]}}'
            'incorrect json'                                     | '{" }'
    }

    def 'Parsing json data with space.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        and: 'some json data with space in the array elements'
            def jsonDataWithSpacesInArrayElement = TestUtils.getResourceFileContent('bookstore.json')
        when: 'that json data is parsed'
            YangUtils.parseJsonData(jsonDataWithSpacesInArrayElement, schemaContext)
        then: 'no exception thrown'
            noExceptionThrown()
    }

    def 'Parsing xPath to nodeId for #scenario.'() {
        when: 'xPath is parsed'
            def result = YangUtils.xpathToNodeIdSequence(xPath)
        then: 'result represents an array of expected identifiers'
            assert result == expectedNodeIdentifier
        where: 'the following parameters are used'
            scenario                                       | xPath                                                               || expectedNodeIdentifier
            'container xpath'                              | '/test-tree'                                                        || ['test-tree']
            'xpath contains list attribute'                | '/test-tree/branch[@name=\'Branch\']'                               || ['test-tree','branch']
            'xpath contains list attributes with /'        | '/test-tree/branch[@name=\'/Branch\']/categories[@id=\'/broken\']'  || ['test-tree','branch','categories']
    }

    def 'Parsing with modified data (CPS-1433).'() {
        given: 'the model (zip) files'
            def multipartFile = new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                    getClass().getResource("/owb-msa221.zip").getBytes())
        and: 'the schema context'
            def yangResourceNameToContent = extractYangResources(multipartFile)
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        and: 'the json data wherein "circuit-pack-name" is removed'
            def jsonData = TestUtils.getResourceFileContent("openroadm_no_circuitPackName.json");
        when: 'data is parsed'
            YangUtils.parseJsonData(jsonData, schemaContext)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Parsing data when #errorScenario (CPS-1433).'() {
        given: 'the model (zip) files'
            def multipartFile = new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                    getClass().getResource("/owb-msa221.zip").getBytes())
        and: 'the schema context'
            def yangResourceNameToContent = extractYangResources(multipartFile)
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        and: 'the json data wherein #errorScenario'
            def jsonData = TestUtils.getResourceFileContent(data);
        when: 'data is parsed'
            YangUtils.parseJsonData(jsonData, schemaContext)
        then: 'exception is thrown'
            thrown(exception)
        where:
            errorScenario                               |  data                                       |   exception
            'data contains slashes as original'         |  "openroadm_with_slashes_original.json"     |   DataValidationException
            'data has no slashes'                       |  "openroadm_no_slashes.json"                |   DataValidationException
            'data with "circuit-pack-name" included'    |  "openroadm_with_circuitPackName.json"      |   DataValidationException
    }

//For debugging only: following class/method added to support zip file extraction
    def extractYangResources(multipartFile) {
        final ImmutableMap.Builder<String, String> yangResourceMapBuilder = ImmutableMap.builder();
        final var zipFileSizeValidator = new ZipFileSizeValidator();
        try (
                final var inputStream = multipartFile.getInputStream();
                final var zipInputStream = new ZipInputStream(inputStream);
        ) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                extractZipEntryToMapIfApplicable(yangResourceMapBuilder, zipEntry, zipInputStream,
                        zipFileSizeValidator);
            }
            zipInputStream.closeEntry();

        } catch (final IOException e) {
            throw new CpsException("Cannot extract resources from zip archive.", e.getMessage(), e);
        }
        zipFileSizeValidator.validateSizeAndEntries();
        try {
            final Map<String, String> yangResourceMap = yangResourceMapBuilder.build();
            if (yangResourceMap.isEmpty()) {
                throw new ModelValidationException("Archive contains no YANG resources.",
                        String.format("Archive contains no files having %s extension.", YANG_FILE_EXTENSION));
            }
            return yangResourceMap;

        } catch (final IllegalArgumentException e) {
            throw new ModelValidationException("Invalid ZIP archive content.",
                    "Multiple resources with same name detected.", e);
        }
    }

    def extractZipEntryToMapIfApplicable(
            final ImmutableMap.Builder<String, String> yangResourceMapBuilder, final ZipEntry zipEntry,
            final ZipInputStream zipInputStream, final ZipFileSizeValidator zipFileSizeValidator) throws IOException {
        zipFileSizeValidator.setCompressedSize(zipEntry.getCompressedSize());
        final String yangResourceName = extractResourceNameFromPath(zipEntry.getName());
        if (zipEntry.isDirectory() || !resourceNameEndsWithExtension(yangResourceName, ".yang")) {
            return;
        }
        yangResourceMapBuilder.put(yangResourceName, extractYangResourceContent(zipInputStream, zipFileSizeValidator));
    }

    def extractResourceNameFromPath(path) {
        return path == null ? "" : path.replaceAll("^.*[\\\\/]", "");
    }

    def resourceNameEndsWithExtension(resourceName,extension) {
        return resourceName != null && resourceName.toLowerCase(Locale.ENGLISH).endsWith(extension);
    }

    def extractYangResourceContent(MultipartFile multipartFile) {
        try {
            return new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new CpsException("Cannot read the resource file.", e.getMessage(), e);
        }
    }

    def extractYangResourceContent(final ZipInputStream zipInputStream,
                                   final ZipFileSizeValidator zipFileSizeValidator) throws IOException {
        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            var totalSizeEntry = 0;
            int numberOfBytesRead;
            final var buffer = new byte[1024];
            zipFileSizeValidator.incrementTotalEntryInArchive();
            while ((numberOfBytesRead = zipInputStream.read(buffer, 0, 1024)) > 0) {
                byteArrayOutputStream.write(buffer, 0, numberOfBytesRead);
                totalSizeEntry += numberOfBytesRead;
                zipFileSizeValidator.updateTotalSizeArchive(totalSizeEntry);
                zipFileSizeValidator.validateCompresssionRatio(totalSizeEntry);
            }
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        }
    }


}

//For debugging only: following class/method added to support zip file extraction

class ZipFileSizeValidator {

    private static final int THRESHOLD_ENTRIES = 10000;
    private static final int THRESHOLD_SIZE = 100000000;
    private static final double THRESHOLD_RATIO = 40;
    private static final String INVALID_ZIP = "Invalid ZIP archive content.";

    private int totalSizeArchive = 0;
    private int totalEntryInArchive = 0;
    private long compressedSize = 0;

    /**
     * Increment the totalEntryInArchive by 1.
     */
    void incrementTotalEntryInArchive() {
        totalEntryInArchive++;
    }

    /**
     * Update the totalSizeArchive by numberOfBytesRead.
     * @param numberOfBytesRead the number of bytes of each entry
     */
    void updateTotalSizeArchive(final int numberOfBytesRead) {
        totalSizeArchive += numberOfBytesRead;
    }

    /**
     * Validate the total Compression size of the zip.
     * @param totalEntrySize the size of the unzipped entry.
     */
    void validateCompresssionRatio(final int totalEntrySize) {
        final double compressionRatio = (double) totalEntrySize / compressedSize;
        if (compressionRatio > THRESHOLD_RATIO) {
            throw new ModelValidationException(INVALID_ZIP,
                    String.format("Ratio between compressed and uncompressed data exceeds the CPS limit"
                            + " %s.", THRESHOLD_RATIO));
        }
    }

    /**
     * Validate the total Size and number of entries in the zip.
     */
    void validateSizeAndEntries() {
        if (totalSizeArchive > THRESHOLD_SIZE) {
            throw new ModelValidationException(INVALID_ZIP,
                    String.format("The uncompressed data size exceeds the CPS limit %s bytes.", THRESHOLD_SIZE));
        }
        if (totalEntryInArchive > THRESHOLD_ENTRIES) {
            throw new ModelValidationException(INVALID_ZIP,
                    String.format("The number of entries in the archive exceeds the CPS limit %s.",
                            THRESHOLD_ENTRIES));
        }
    }

    void setCompressedSize(newCompressedSize){
        compressedSize = newCompressedSize;
    }
}
