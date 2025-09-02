/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.init.actuator

import spock.lang.Specification;

class ReadinessHealthIndicatorSpec extends Specification {

    def mockReadinessManager = new ReadinessManager()
    def objectUnderTest = new ReadinessHealthIndicator(mockReadinessManager)

    def 'Inventory model upgrade and data migration is in progress'() {
        given: 'readiness manager reports ready'
            mockReadinessManager.markReady()
        when: 'health check is invoked'
            def health = objectUnderTest.health()
        then: 'health status is UP'
            assert health.status.code == 'UP'
            assert health.details['Inventory-Model'] == 'Ready: Migration is done'
    }

    def 'Inventory model upgrade and data migration is done'() {
        given: 'readiness manager reports not ready'
            mockReadinessManager.markNotReady()
        when: 'health check is invoked'
            def health = objectUnderTest.health()
        then: 'health status is DOWN'
            assert health.status.code == 'DOWN'
            assert health.details['Inventory-Model'] == 'Not ready: Migration/Rollback is in progress'
    }
}
