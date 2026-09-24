/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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
import {check} from 'k6';

export const TEST_PROFILE = __ENV.TEST_PROFILE ? __ENV.TEST_PROFILE : 'kpi'
export const testConfig = JSON.parse(open(`../config/${TEST_PROFILE}.json`));
export const scenarioMetaData = JSON.parse(open(`../config/scenario-metadata.json`));
export const CPS_CORE_BASE_URL = __ENV.CPS_CORE_BASE_URL || testConfig.cpsCoreBaseUrl;
export const CONTAINER_COOL_DOWN_TIME_IN_SECONDS = testConfig.containerCoolDownTimeInSeconds || 10;
export const CONTENT_TYPE_JSON_PARAM = {'Content-Type': 'application/json'};
export const CONTENT_TYPE_JSON_PATCH_PARAM = {'Content-Type': 'application/json-patch+json'};

/**
 * Helper function to perform GET requests with metric tags.
 * @param {string} url - The URL to which the GET request will be sent.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP GET request.
 */
export function performGetRequest(url, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.get(url, {tags: metricTags});
}

/**
 * Helper function to perform POST requests with JSON payload and content type.
 * @param {string} url - The URL to send the POST request to.
 * @param {Object} payload - The JSON payload to send in the POST request.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP POST request.
 */
export function performPostRequest(url, payload, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.post(url, payload, {
        headers: CONTENT_TYPE_JSON_PARAM,
        tags: metricTags
    });
}

/**
 * Helper function to perform PATCH requests with metric tags.
 * @param {string} url - The URL to which the PATCH request will be sent.
 * @param {Object} payload - The JSON payload to send in the PATCH request.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP PATCH request.
 */
export function performPatchRequest(url, payload, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.request('PATCH', url, payload, {
        headers: CONTENT_TYPE_JSON_PATCH_PARAM,
        tags: metricTags
    });
}

/**
 * Helper function to perform PUT requests with metric tags.
 * @param {string} url - The URL to which the PUT request will be sent.
 * @param {Object} payload - The JSON payload to send in the PUT request.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP PUT request.
 */
export function performPutRequest(url, payload, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.put(url, payload, {
        headers: CONTENT_TYPE_JSON_PARAM,
        tags: metricTags
    });
}

/**
 * Helper function to perform DELETE requests with metric tags.
 * @param {string} url - The URL to which the DELETE request will be sent.
 * @param {string} metricTag - A tag for the metric endpoint.
 * @returns {Object} The response from the HTTP DELETE request.
 */
export function performDeleteRequest(url, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.del(url, null, {tags: metricTags});
}

export function makeCustomSummaryReport(testResults, scenarioConfig) {
    const summaryCsvLines = [
        '#,Test Name,Unit,Fs Requirement,Current Expectation,Actual',
        ...scenarioMetaData.map(kpiTest => {
            return makeSummaryCsvLine(
                kpiTest.testNumber,
                kpiTest.testName,
                kpiTest.unit,
                kpiTest.measurementName,
                kpiTest.currentExpectation,
                testResults,
                scenarioConfig
            );
        })
    ];
    return summaryCsvLines.join('\n') + '\n';
}

function makeSummaryCsvLine(testNumber, testName, unit, measurementName, currentExpectation, testResults, scenarioConfig) {
    const thresholdArray = JSON.parse(JSON.stringify(scenarioConfig.thresholds[measurementName]));
    const thresholdString = thresholdArray[0];
    const [thresholdKey, thresholdOperator, thresholdValue] = thresholdString.split(/\s+/);
    const rawValue = testResults.metrics[measurementName].values[thresholdKey];
    const decimals = (measurementName === 'http_req_failed') ? 6 : 3;
    const actualValue = rawValue.toFixed(decimals);
    return `${testNumber},${testName},${unit},${thresholdValue},${currentExpectation},${actualValue}`;
}

/**
 * Validates that the response has the expected status code and records the request duration.
 * @param {Object} httpResponse
 * @param {number} expectedStatusCode
 * @param {string} checkLabel
 * @param {Trend} testTrend
 */
export function validateResponseAndRecordMetric(httpResponse, expectedStatusCode, checkLabel, testTrend) {
    const isSuccess = check(httpResponse, {
        [checkLabel]: () => httpResponse.status === expectedStatusCode,
    });

    if (isSuccess) {
        testTrend.add(httpResponse.timings.duration);
    } else {
        logDetailedFailure(httpResponse, checkLabel);
    }
}

function logDetailedFailure(httpResponse, checkLabel) {
    const {status, url, body} = httpResponse;
    const trimmedBody = typeof body === 'string' ? body.trim() : '';

    if (!trimmedBody) {
        console.error(`❌ ${checkLabel}: Status ${status}. Empty response body. URL: ${url}`);
        return;
    }

    try {
        const responseJson = JSON.parse(trimmedBody);
        const errorMessage = responseJson && responseJson.message ? responseJson.message : 'No message';
        const errorDetails = responseJson && responseJson.details ? responseJson.details : 'No details';
        console.error(`❌ ${checkLabel}: Status ${status}, Message: ${errorMessage}, Details: ${errorDetails}, URL: ${url}`);
    } catch (e) {
        const bodyPreview = trimmedBody.length > 500 ? trimmedBody.slice(0, 500) + '... [truncated]' : trimmedBody;
        console.error(`❌ ${checkLabel}: Status ${status}, URL: ${url}. Response is not valid JSON.\n↪️ Raw body preview:\n${bodyPreview}\n✳️ Parse error: ${e.message}`);
    }
}
