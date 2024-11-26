/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.exceptions


import spock.lang.Specification

class CpsExceptionsSpec extends Specification {
    def dataspaceName = 'some data space'
    def anchorName = 'some anchor'
    def schemaSetName = 'some schema set'
    def rootCause = new Throwable()
    def providedMessage = 'some message'
    def providedDetails = 'some details'
    def xpath = 'some xpath'
    def additionalInformation = 'some information'

    def 'Creating an exception that the Anchor already exist.'() {
        given: 'an exception dat the Anchor already exist is created'
            def exception = new AlreadyDefinedException('Anchor', anchorName, dataspaceName, rootCause)
        expect: 'the exception details contains the correct message with Anchor name and Dataspace name'
            exception.details == "Anchor with name ${anchorName} already exists for ${dataspaceName}."
        and: 'the correct root cause is maintained'
            exception.cause == rootCause
    }

    def 'Creating an exception that the dataspace already exists.'() {
        given: 'an exception that the dataspace already exists is created'
            def exception = new AlreadyDefinedException(dataspaceName, rootCause)
        expect: 'the exception details contains the correct message with dataspace name'
            exception.details == "${dataspaceName} already exists."
        and: 'the correct root cause is maintained'
            exception.cause == rootCause
    }

    def 'Creating a exception that a dataspace is not found.'() {
        expect: 'the exception details contains the correct message with dataspace name'
            (new DataspaceNotFoundException(dataspaceName)).details
                    == "Dataspace with name ${dataspaceName} does not exist."
    }

    def 'Creating a data validation exception with root cause.'() {
        given: 'a data validation exception is created'
            def exception = new DataValidationException(providedMessage, providedDetails, rootCause)
        expect: 'the exception has the provided message'
            exception.message == providedMessage
        and: 'the exception has the provided details'
            exception.details == providedDetails
        and: 'the correct root cause is maintained'
            exception.cause == rootCause
    }

    def 'Creating a data validation exception.'() {
        given: 'a data validation exception is created'
            def exception = new DataValidationException(providedMessage, providedDetails)
        expect: 'the exception has the provided message'
            exception.message == providedMessage
        and: 'the exception has the provided details'
            exception.details == providedDetails
    }

    def 'Creating a model validation exception.'() {
        given: 'a data validation exception is created'
            def exception = new ModelValidationException(providedMessage, providedDetails)
        expect: 'the exception has the provided message'
            exception.message == providedMessage
        and: 'the exception has the provided details'
            exception.details == providedDetails
    }

    def 'Creating a model validation exception with a root cause.'() {
        given: 'a model validation exception is created'
            def exception = new ModelValidationException(providedMessage, providedDetails, rootCause)
        expect: 'the exception has the provided message'
            exception.message == providedMessage
        and: 'the exception has the provided details'
            exception.details == providedDetails
        and: 'the correct root cause is maintained'
            exception.cause == rootCause
    }

    def 'Creating a exception for an object not found in a dataspace.'() {
        def descriptionOfObject = 'some object'
        expect: 'the exception details contains the correct message with dataspace name and description of the object'
            (new NotFoundInDataspaceException(dataspaceName, descriptionOfObject)).details
                    == "${descriptionOfObject} does not exist in dataspace ${dataspaceName}."
    }

    def 'Creating a exception that a schema set cannot be found.'() {
        expect: 'the exception details contains the correct message with dataspace and schema set names'
            (new SchemaSetNotFoundException(dataspaceName, schemaSetName)).details
                    == "Schema Set with name ${schemaSetName} was not found for dataspace ${dataspaceName}."
    }

    def 'Creating a exception that an anchor cannot be found.'() {
        expect: 'the exception details contains the correct message with dataspace and anchor name'
            (new AnchorNotFoundException(anchorName, dataspaceName)).details
                    == "Anchor with name ${anchorName} does not exist in dataspace ${dataspaceName}."
    }

    def 'Creating an exception that the schema set being used and cannot be deleted.'() {
        expect: 'the exception details contains the correct message with dataspace and schema set names'
            (new SchemaSetInUseException(dataspaceName, schemaSetName)).details
                    == ("Schema Set with name ${schemaSetName} in dataspace ${dataspaceName} is having" +
                " Anchor records associated.")
    }

    def 'Creating a exception that a datanode with a specified xpath does not exist.'() {
        expect: 'the exception details contains the correct message with dataspace name and xpath.'
            (new DataNodeNotFoundException(dataspaceName, anchorName, xpath)).details
                    == "DataNode with xpath ${xpath} was not found for anchor ${anchorName} and dataspace ${dataspaceName}."
    }

    def 'Creating a exception that a datanode does not exist.'() {
        expect: 'the exception details contains the correct message with dataspace name and anchor.'
            (new DataNodeNotFoundException(dataspaceName, anchorName)).details
                    == "DataNode not found for anchor ${anchorName} and dataspace ${dataspaceName}."
    }

    def 'Creating a exception that a datanode with a specified xpath with additional information does not exist.'() {
        expect: 'the exception details contains the correct message with dataspace name and anchor.'
        (new DataNodeNotFoundException(dataspaceName, anchorName, xpath, additionalInformation)).details
                == "DataNode with xpath ${xpath} was not found for anchor ${anchorName} and dataspace ${dataspaceName}, ${additionalInformation}."
    }

    def 'Creating a exception that a dataspace already exists.'() {
        expect: 'the exception details contains the correct message with dataspace name.'
            (AlreadyDefinedException.forDataspace(dataspaceName, rootCause)).details
                    == "${dataspaceName} already exists."
    }

    def 'Creating a exception that a anchor already exists.'() {
        expect: 'the exception details contains the correct message with anchor name and dataspace name.'
            (AlreadyDefinedException.forAnchor(anchorName, dataspaceName, rootCause)).details
                    == "Anchor with name ${anchorName} already exists for ${dataspaceName}."
    }

    def 'Creating a exception that data nodes already exist.'() {
        expect: 'the exception details contains the correct message with xpath and dataspace name.'
            (AlreadyDefinedException.forDataNodes([xpath], anchorName)).details
                    == "1 data node(s) already exist for ${anchorName}."
    }

    def 'Creating a exception that a schema set already exists.'() {
        expect: 'the exception details contains the correct message with schema set and dataspace name.'
            (AlreadyDefinedException.forSchemaSet(schemaSetName, dataspaceName, rootCause)).details
                    == "Schema Set with name ${schemaSetName} already exists for ${dataspaceName}."
    }

    def 'Creating a cps path exception.'() {
        given: 'a cps path exception is created'
            def exception = new CpsPathException(providedDetails)
        expect: 'the exception has the provided details'
            exception.details == providedDetails
    }

    def 'Creating an exception that the dataspace is being used and cannot be deleted.'() {
        given: 'a dataspace in use exception is created'
            def exception = new DataspaceInUseException(dataspaceName,providedDetails)
        expect: 'the exception has the correct message with dataspace name and provided details'
            exception.message == "Dataspace with name ${dataspaceName} is being used."
            exception.details == providedDetails
    }
}
