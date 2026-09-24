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

import http from 'k6/http';
import encoding from 'k6/encoding';

export const TEST_PROFILE = __ENV.TEST_PROFILE ? __ENV.TEST_PROFILE : 'functional';
export const testConfig = JSON.parse(open(`../config/${TEST_PROFILE}.json`));

export const DMI_BASE_URL = __ENV.DMI_BASE_URL || testConfig.dmiBaseUrl;
export const DMI_USERNAME = __ENV.DMI_USERNAME || testConfig.dmiUsername;
export const DMI_PASSWORD = __ENV.DMI_PASSWORD || testConfig.dmiPassword;

/**
 * CPS-NCMP's address. Used for the NCMP health checks, for polling the cm-handle
 * state, and to de-register the cm-handle (DMI's inventory API can register but
 * not remove).
 */
export const NCMP_BASE_URL = __ENV.NCMP_BASE_URL || testConfig.ncmpBaseUrl;

/**
 * The in-cluster address NCMP uses to reach DMI. Required in the de-registration
 * body, and deliberately not the NodePort URL: NCMP resolves it from inside the
 * cluster.
 */
export const DMI_PLUGIN_IN_CLUSTER_URL = __ENV.DMI_PLUGIN_IN_CLUSTER_URL || testConfig.dmiPluginInClusterUrl;

/**
 * The cm handle backed by the netconf simulator (pnfsim) mounted into SDNC.
 */
export const CM_HANDLE = __ENV.CM_HANDLE || testConfig.cmHandle;

/**
 * Number of distinct YANG modules pnfsim exposes.
 *
 * Note DMI returns each module twice (44 entries for 22 modules), so assertions
 * count unique module names rather than raw array length.
 */
export const EXPECTED_NUMBER_OF_MODULES = __ENV.EXPECTED_NUMBER_OF_MODULES
    ? parseInt(__ENV.EXPECTED_NUMBER_OF_MODULES)
    : testConfig.expectedNumberOfModules;

export const CM_HANDLE_READY_TIMEOUT_IN_SECONDS = testConfig.cmHandleReadyTimeoutInSeconds || 180;
export const CM_HANDLE_READY_POLL_INTERVAL_IN_SECONDS = testConfig.cmHandleReadyPollIntervalInSeconds || 5;

export const CONTENT_TYPE_JSON_PARAM = {'Content-Type': 'application/json'};

export const DMI_AUTH_HEADER = `Basic ${encoding.b64encode(`${DMI_USERNAME}:${DMI_PASSWORD}`)}`;

/**
 * Helper function to perform POST requests against DMI with basic auth and a
 * metric tag.
 *
 * @param {string} url - The URL to send the POST request to.
 * @param {string} payload - The JSON payload to send.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP POST request.
 */
export function performPostRequest(url, payload, metricTag) {
    return http.post(url, payload, {
        headers: {...CONTENT_TYPE_JSON_PARAM, Authorization: DMI_AUTH_HEADER},
        tags: {endpoint: metricTag},
    });
}

/**
 * Helper function to perform GET requests with a metric tag.
 *
 * @param {string} url - The URL to send the GET request to.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP GET request.
 */
export function performGetRequest(url, metricTag) {
    return http.get(url, {tags: {endpoint: metricTag}});
}

/**
 * Records a check and logs response detail when it fails, so a CI failure points
 * at the cause without needing to re-run locally.
 *
 * @param {Object} httpResponse - The response to validate.
 * @param {number} expectedStatusCode - The status code the endpoint contract defines.
 * @param {string} checkLabel - The check description.
 * @returns {boolean} True when the response matched the expected status.
 */
export function validateResponseStatus(httpResponse, expectedStatusCode, checkLabel) {
    const isExpectedStatus = httpResponse.status === expectedStatusCode;
    if (!isExpectedStatus) {
        const body = typeof httpResponse.body === 'string' ? httpResponse.body.trim() : '';
        const bodyPreview = body.length > 500 ? `${body.slice(0, 500)}... [truncated]` : body;
        console.error(`❌ ${checkLabel}: expected status ${expectedStatusCode} but got `
            + `${httpResponse.status}. URL: ${httpResponse.url}. Body: ${bodyPreview || '(empty)'}`);
    }
    return isExpectedStatus;
}
