/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import {randomIntBetween} from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import http from 'k6/http';
import {check} from 'k6';
import {Trend} from 'k6/metrics';

export const TEST_PROFILE = __ENV.TEST_PROFILE ? __ENV.TEST_PROFILE : 'kpi'
export const HOST_TYPE = __ENV.DEPLOYMENT_TYPE ? __ENV.DEPLOYMENT_TYPE : 'dockerHosts'

// Load environment and profile configurations
const environmentConfig = JSON.parse(open(`../environments/${HOST_TYPE === 'dockerHosts' ? 'docker' : 'kubernetes'}/config.json`));
const profileConfig = JSON.parse(open(`../profiles/${TEST_PROFILE}/scenarios.json`));
export const scenarioMetaData = JSON.parse(open(`../ncmp/config/scenario-metadata.json`));

export const KAFKA_BOOTSTRAP_SERVERS = environmentConfig.kafkaBootstrapServer;
export const NCMP_BASE_URL = environmentConfig.ncmpBaseUrl;
export const DMI_PLUGIN_URL = environmentConfig.dmiStubUrl;
export const CONTAINER_COOL_DOWW_TIME_IN_SECONDS = environmentConfig.containerCoolDownTimeInSeconds || 10;
export const LEGACY_BATCH_TOPIC_NAME = 'legacy_batch_topic';
export const TOTAL_CM_HANDLES = __ENV.TOTAL_CM_HANDLES ? parseInt(__ENV.TOTAL_CM_HANDLES) : 50000;
export const REGISTRATION_BATCH_SIZE = 2000;
export const READ_DATA_FOR_CM_HANDLE_DELAY_MS = 300; // must have same value as in docker-compose.yml
export const WRITE_DATA_FOR_CM_HANDLE_DELAY_MS = 670; // must have same value as in docker-compose.yml
export const CONTENT_TYPE_JSON_PARAM = {'Content-Type': 'application/json'};
export const LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE = 200; // Note: a maximum batch size of 200 implemented in production code!
export const MODULE_SET_TAGS = ['tagA', 'tagB', 'tagC', 'tagD', 'tagE'];

/**
 * Generates a batch of CM-handle IDs based on batch size and number.
 * @param {number} batchSize - Size of each batch.
 * @param {number} batchNumber - Number of the batch.
 * @returns {string[]} Array of CM-handle IDs, for example ['ch-201', 'ch-202' ... 'ch-300']
 */
export function makeBatchOfCmHandleIds(batchSize, batchNumber) {
    const startIndex = 1 + batchNumber * batchSize;
    return Array.from({ length: batchSize }, (_, i) => `ch-${startIndex + i}`);
}

/**
 * Generates an unordered batch of Alternate IDs.
 * The batch size is determined by `LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE`,
 * and the IDs are generated within the range of `TOTAL_CM_HANDLES`.
 *
 * @returns {string[]} Array of Alternate IDs, for example,
 * ['Region=NorthAmerica,Segment=8', 'Region=NorthAmerica,Segment=2' ... 'Region=NorthAmerica,Segment=32432']
 */
export function makeRandomBatchOfAlternateIds() {
    const alternateIds = new Set();
    while (alternateIds.size < LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE) {
        alternateIds.add(getRandomAlternateId());
    }
    return Array.from(alternateIds)
}

/**
 * Generates a random CM Handle alternate ID.
 *
 * This function selects a random CM Handle ID between 1 and TOTAL_CM_HANDLES (inclusive)
 * and returns its corresponding alternate ID by invoking `getAlternateId(id)`.
 *
 * @returns {string} A CM Handle alternate ID derived from a randomly selected CM Handle ID.
 */
export function getRandomAlternateId() {
    let randomCmHandleId = randomIntBetween(1, TOTAL_CM_HANDLES);
    return getAlternateId(randomCmHandleId);
}

export function getAlternateId(cmHandleNumericId) {
    return `/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${cmHandleNumericId}/ManagedElement=MyManagedElement${cmHandleNumericId}`;
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
 * Helper function to perform GET requests with metric tags.
 *
 * This function sends an HTTP GET request to the specified URL and attaches
 * a metric tag to the request, which is useful for monitoring and analytics.
 *
 * @param {string} url - The URL to which the GET request will be sent.
 * @param {string} metricTag - A string representing the metric tag to associate with the request.
 *                             This tag is used for monitoring and tracking the request.
 * @returns {Object} The response from the HTTP GET request. The response includes the status code,
 *                   headers, body, and other related information.
 */
export function performGetRequest(url, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.get(url, {tags: metricTags});
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
    const actualValue = testResults.metrics[measurementName].values[thresholdKey].toFixed(3);
    return `${testNumber},${testName},${unit},${thresholdValue},${currentExpectation},${actualValue}`;
}

/**
 * Processes HTTP response and records duration minus known overhead (e.g., artificial delay).
 *
 * @param {Object} httpResponse
 * @param {number} expectedStatusCode
 * @param {string} checkLabel
 * @param {number} delayInMs - Overhead to subtract
 * @param {Trend} testTrend
 */
export function validateResponseAndRecordMetricWithOverhead(httpResponse, expectedStatusCode, checkLabel, delayInMs, testTrend) {
    recordMetricIfResponseValid(httpResponse, expectedStatusCode, checkLabel, testTrend,
        (httpResponse) => httpResponse.timings.duration - delayInMs
    );
}

/**
 * Validates that the JSON response is an array of expected length and records duration.
 *
 * @param {Object} httpResponse
 * @param {number} expectedStatusCode
 * @param {string} checkLabel
 * @param {number} expectedArrayLength
 * @param {Trend} testTrend
 */
export function validateResponseAndRecordMetric(httpResponse, expectedStatusCode, checkLabel, expectedArrayLength, testTrend) {
    recordMetricIfResponseValid(httpResponse, expectedStatusCode, checkLabel, testTrend, (response) => response.timings.duration, (response) => {
        const status = response.status;
        const body = typeof response.body === 'string' ? response.body.trim() : '';
        if (!body) {
            return {valid: false, reason: `Status ${status} - Empty response body`};
        }
        try {
            const arrayLength = response.json('#');
            const valid = arrayLength === expectedArrayLength;
            return {
                valid,
                reason: valid ? undefined : `Status ${status} - Expected array length ${expectedArrayLength}, but got ${arrayLength}`
            };
        } catch (e) {
            return {valid: false, reason: `Status ${status} - JSON parse error: ${e.message}`};
        }
    });
}

function recordMetricIfResponseValid(httpResponse, expectedStatusCode, checkLabel, testTrend, metricValueExtractor, customValidatorFn = () => ({
    valid: true,
    reason: undefined
})) {
    const isExpectedStatusMatches = httpResponse.status === expectedStatusCode;
    const {valid: isCustomValidationPasses, reason = ""} = customValidatorFn(httpResponse);

    const isSuccess = check(httpResponse, {
        [checkLabel]: () => isExpectedStatusMatches && isCustomValidationPasses,
    });

    if (isSuccess) {
        testTrend.add(metricValueExtractor(httpResponse));
    } else {
        logDetailedFailure(httpResponse, isExpectedStatusMatches, checkLabel, reason);
    }
}

function logDetailedFailure(httpResponse, isExpectedStatusMatches, checkLabel, customReason = "") {
    const {status, url, body} = httpResponse;
    const trimmedBody = typeof body === 'string' ? body.trim() : '';

    // If status is okay but custom check failed, log that reason only
    if (isExpectedStatusMatches && customReason) {
        console.error(`❌ ${checkLabel}: Custom validation failed. Reason: ${customReason}. URL: ${url}`);
        return;
    }

    // Categorize status
    let errorCategory;
    if (status >= 100 && status < 200) {
        errorCategory = `Informational Response (${status})`;
    } else if (status >= 300 && status < 400) {
        errorCategory = `Redirection (${status})`;
    } else if (status >= 400 && status < 500) {
        errorCategory = 'Client Error (4xx)';
    } else if (status >= 500 && status < 600) {
        errorCategory = 'Server Error (5xx)';
    } else if (status === 0) {
        errorCategory = 'Network Error or Timeout (status = 0 - likely no HTTP response received)';
    } else {
        errorCategory = `Unexpected Status (${status})`;
    }

    if (!trimmedBody) {
        console.error(`❌ ${checkLabel}: ${errorCategory}. Empty response body. URL: ${url}`);
        return;
    }

    try {
        const responseJson = JSON.parse(trimmedBody);
        const errorMessage = responseJson && responseJson.message ? responseJson.message : 'No message';
        const errorDetails = responseJson && responseJson.details ? responseJson.details : 'No details';
        console.error(`❌ ${checkLabel}: ${errorCategory}. Status: ${status}, Message: ${errorMessage}, Details: ${errorDetails}, URL: ${url}`);
    } catch (e) {
        const bodyPreview = trimmedBody.length > 500 ? trimmedBody.slice(0, 500) + '... [truncated]' : trimmedBody;
        console.error(`❌ ${checkLabel}: ${errorCategory}. Status: ${status}, URL: ${url}. Response is not valid JSON.\n↪️ Raw body preview:\n${bodyPreview}\n✳️ Parse error: ${e.message}`);
    }
}