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
 * Checks a health response reports status UP.
 *
 * A 200 alone is not sufficient: the actuator can answer 200 while a component is
 * degraded, so the status field is asserted too.
 *
 * @param {Object} httpResponse - The response from a health endpoint.
 * @returns {boolean} True when the response is 200 and reports UP.
 */
export function isHealthy(httpResponse) {
    if (httpResponse.status !== 200) {
        return false;
    }
    try {
        return httpResponse.json('status') === 'UP';
    } catch (error) {
        console.error(`❌ Health response is not valid JSON: ${error.message}`);
        return false;
    }
}
