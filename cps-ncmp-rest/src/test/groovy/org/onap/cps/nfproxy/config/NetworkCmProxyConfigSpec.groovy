/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
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

package org.onap.cps.nfproxy.config

import org.onap.cps.ncmp.config.NetworkCmProxyConfig
import spock.lang.Specification
import springfox.documentation.spring.web.plugins.Docket

class NetworkCmProxyConfigSpec extends Specification {
    def objectUnderTest = new NetworkCmProxyConfig()

    def 'NfProxy configuration has a Docket API.'() {
        expect: 'the NfProxy configuration has a Docket API'
            objectUnderTest.api() instanceof Docket
    }
}
