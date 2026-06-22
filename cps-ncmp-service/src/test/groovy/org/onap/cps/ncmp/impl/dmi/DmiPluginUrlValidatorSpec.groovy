/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.dmi

import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import spock.lang.Specification

class DmiPluginUrlValidatorSpec extends Specification {

    def objectUnderTest = new DmiPluginUrlValidator('')

    def 'Validation of valid DMI plugin URLs.'() {
        given: 'a registration with a valid URL'
            def registration = new DmiPluginRegistration(dmiPlugin: url)
        when: 'validation is performed'
            objectUnderTest.validateDmiPluginUrls(registration)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'the following URLs are valid'
            scenario            | url
            'http URL'          | 'http://my-dmi-service:8080'
            'https URL'         | 'https://my-dmi-service.svc.cluster.local'
            'http with path'    | 'http://dmi-plugin:8080/some-path'
            'localhost'         | 'http://localhost:8080'
            'loopback'          | 'http://127.0.0.1:8080'
            'empty string'      | ''
            'null value'        | null
    }

    def 'Validate with invalid scheme.'() {
        given: 'a registration with a non-http(s) URL'
            def registration = new DmiPluginRegistration(dmiPlugin: url)
        when: 'validation is performed'
            objectUnderTest.validateDmiPluginUrls(registration)
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
        where: 'the following URLs are invalid'
            scenario         | url
            'invalid scheme' | 'ftp://some-host'
            'no scheme'      | 'just-a-hostname'
            'malformed URI'  | 'http://host with spaces'
            'missing host'   | 'http://:8080'
    }

    def 'Validation with all DMI plugin URL fields.'() {
        given: 'a registration with an invalid URL in the #fieldName field'
            def registration = new DmiPluginRegistration()
            registration."$fieldName" =  'invalid-scheme://some-host'
        when: 'validation is performed'
            objectUnderTest.validateDmiPluginUrls(registration)
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
        where: 'each field is validated'
            fieldName << ['dmiPlugin', 'dmiDataPlugin', 'dmiModelPlugin',
                          'dmiDatajobsReadPlugin', 'dmiDatajobsWritePlugin']
    }

    def 'Validation adhering to allowed URL pattern.'() {
        given: 'a validator with a pattern allowing only cluster-local URLs'
            def restrictedValidator = new DmiPluginUrlValidator('.*dmi.*')
        and: 'a registration with a non-matching URL'
            def registration = new DmiPluginRegistration(dmiPlugin: 'http://external-host.com:8080')
        when: 'validation is performed'
            restrictedValidator.validateDmiPluginUrls(registration)
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
    }

    def 'Validation not adhering to allowed URL pattern.'() {
        given: 'a validator with a pattern allowing only cluster-local URLs'
            def restrictedValidator = new DmiPluginUrlValidator('.*dmi.*')
        and: 'a registration with a matching URL'
            def registration = new DmiPluginRegistration(dmiPlugin: url)
        when: 'validation is performed'
            restrictedValidator.validateDmiPluginUrls(registration)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'the following URLs match the pattern'
            scenario        | url
            'matching http' | 'http://my-dmi.svc.cluster.local:8080/path'
            'matching https'| 'https://dmi-plugin.svc.cluster.local'
    }

    def 'Validation with no pattern configured.'() {
        given: 'a validator with no pattern (empty string)'
            def noPatternValidator = new DmiPluginUrlValidator('')
        and: 'a registration with an external URL'
            def registration = new DmiPluginRegistration(dmiPlugin: 'http://any-external-host.com:8080')
        when: 'validation is performed'
            noPatternValidator.validateDmiPluginUrls(registration)
        then: 'no exception is thrown'
            noExceptionThrown()
    }
}
