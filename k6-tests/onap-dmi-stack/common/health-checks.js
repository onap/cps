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

import {performGetRequest, DMI_BASE_URL, NCMP_BASE_URL} from './utils.js';

/**
 * Reads the DMI plugin's liveness endpoint.
 *
 * @returns {Object} The HTTP response.
 */
export function getDmiLiveness() {
    return performGetRequest(`${DMI_BASE_URL}/actuator/health/liveness`, 'dmiLiveness');
}

/**
 * Reads the DMI plugin's readiness endpoint.
 *
 * @returns {Object} The HTTP response.
 */
export function getDmiReadiness() {
    return performGetRequest(`${DMI_BASE_URL}/actuator/health/readiness`, 'dmiReadiness');
}

/**
 * Reads CPS-NCMP's liveness endpoint.
 *
 * @returns {Object} The HTTP response.
 */
export function getNcmpLiveness() {
    return performGetRequest(`${NCMP_BASE_URL}/actuator/health/liveness`, 'ncmpLiveness');
}

/**
 * Reads CPS-NCMP's readiness endpoint.
 *
 * @returns {Object} The HTTP response.
 */
export function getNcmpReadiness() {
    return performGetRequest(`${NCMP_BASE_URL}/actuator/health/readiness`, 'ncmpReadiness');
}

/**
 * Reads the DMI plugin's aggregate health endpoint.
 *
 * Distinct from the liveness and readiness probe groups: this reports the overall
 * status including components such as the database and disk space, so a degraded
 * component is visible here even when the probes still pass.
 *
 * @returns {Object} The HTTP response.
 */
export function getDmiAggregateHealth() {
    return performGetRequest(`${DMI_BASE_URL}/actuator/health`, 'dmiAggregateHealth');
}

/**
 * Reads the DMI plugin's Swagger UI page.
 *
 * Returns HTML rather than JSON, so callers assert on the status code only.
 *
 * @returns {Object} The HTTP response.
 */
export function getDmiSwaggerUi() {
    return performGetRequest(`${DMI_BASE_URL}/swagger-ui/index.html`, 'dmiSwaggerUi');
}

/**
 * Checks a health response is 200 and reports status UP.
 *
 * A DOWN component makes the overall status DOWN, which the actuator returns as
 * 503, so the status code catches it. The status field is asserted as well
 * because UNKNOWN is also returned as 200.
 *
 * @param {Object} httpResponse - The response from a health endpoint.
 * @returns {boolean} True when the response is 200 and reports UP.
 */
export function isHealthy(httpResponse) {
    if (httpResponse.status !== 200) {
        console.error(`❌ Health check returned status ${httpResponse.status}. `
            + `URL: ${httpResponse.url}. Body: ${httpResponse.body}`);
        return false;
    }
    try {
        return httpResponse.json('status') === 'UP';
    } catch (error) {
        console.error(`❌ Health response is not valid JSON: ${error.message}`);
        return false;
    }
}
