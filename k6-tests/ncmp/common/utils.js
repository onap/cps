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

export const testConfig = JSON.parse(open(`../config/${__ENV.TEST_PROFILE}.json`));
export const testKpiMetaData = JSON.parse(open(`../config/test-kpi-metadata.json`));
export const KAFKA_BOOTSTRAP_SERVERS = testConfig.hosts.kafkaBootstrapServer;
export const NCMP_BASE_URL = testConfig.hosts.ncmpBaseUrl;
export const DMI_PLUGIN_URL = testConfig.hosts.dmiStubUrl;
export const CONTAINER_UP_TIME_IN_SECONDS = testConfig.hosts.containerUpTimeInSeconds;
export const LEGACY_BATCH_TOPIC_NAME = 'legacy_batch_topic';
export const TOTAL_CM_HANDLES = __ENV.TOTAL_CM_HANDLES ? parseInt(__ENV.TOTAL_CM_HANDLES) : 50000;
export const REGISTRATION_BATCH_SIZE = 2000;
export const READ_DATA_FOR_CM_HANDLE_DELAY_MS = 300; // must have same value as in docker-compose.yml
export const WRITE_DATA_FOR_CM_HANDLE_DELAY_MS = 670; // must have same value as in docker-compose.yml
export const CONTENT_TYPE_JSON_PARAM = {'Content-Type': 'application/json'};
export const LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE = 200;
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
        ...testKpiMetaData.map(test => {
            return makeSummaryCsvLine(
                test.label,
                test.name,
                test.unit,
                test.metric,
                test.cpsAverage,
                testResults,
                scenarioConfig
            );
        })
    ];
    return summaryCsvLines.join('\n') + '\n';
}

function makeSummaryCsvLine(testCase, testName, unit, measurementName, currentExpectation, testResults, scenarioConfig) {
    const thresholdArray = JSON.parse(JSON.stringify(scenarioConfig.thresholds[measurementName]));
    const thresholdString = thresholdArray[0];
    const [thresholdKey, thresholdOperator, thresholdValue] = thresholdString.split(/\s+/);
    const actualValue = testResults.metrics[measurementName].values[thresholdKey].toFixed(3);
    return `${testCase},${testName},${unit},${thresholdValue},${currentExpectation},${actualValue}`;
}

/**
 * Processes HTTP response and records duration minus known overhead (e.g., artificial delay).
 *
 * @param {Object} httpResponse
 * @param {number} expectedStatusCode
 * @param {string} checkLabel
 * @param {number} delayInMs - Overhead to subtract
 * @param {Trend} trendMetric
 */
export function validateResponseAndRecordMetricWithOverhead(httpResponse, expectedStatusCode, checkLabel, delayInMs, trendMetric) {
    recordMetricIfResponseValid(httpResponse, expectedStatusCode, checkLabel, trendMetric,
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
 * @param {Trend} trendMetric
 */
export function validateResponseAndRecordMetric(httpResponse, expectedStatusCode, checkLabel, expectedArrayLength, trendMetric) {
    recordMetricIfResponseValid(httpResponse, expectedStatusCode, checkLabel, trendMetric, (response) => response.timings.duration, (response) => {
        const body = typeof response.body === 'string' ? response.body.trim() : '';
        if (!body) {
            return {valid: false, reason: "Empty response body"};
        }
        try {
            const arrayLength = response.json('#');
            const valid = arrayLength === expectedArrayLength;
            return {
                valid,
                reason: valid ? undefined : `Expected array length ${expectedArrayLength}, but got ${arrayLength}`
            };
        } catch (e) {
            return {valid: false, reason: `JSON parse error: ${e.message}`};
        }
    });
}

function recordMetricIfResponseValid(httpResponse, expectedStatusCode, checkLabel, metricRecorder, metricValueExtractor, customValidatorFn = () => ({
    valid: true,
    reason: undefined
})) {
    const isExpectedStatusMatches = httpResponse.status === expectedStatusCode;
    const {valid: isCustomValidationPasses, reason = ""} = customValidatorFn(httpResponse);

    const isSuccess = check(httpResponse, {
        [checkLabel]: () => isExpectedStatusMatches && isCustomValidationPasses,
    });

    if (isSuccess) {
        metricRecorder.add(metricValueExtractor(httpResponse));
    } else {
        logDetailedFailure(httpResponse, isExpectedStatusMatches, checkLabel, reason);
    }
}

function logDetailedFailure(httpResponse, isExpectedStatusMatches, checkLabel, customReason = "") {
    const {status, url, body} = httpResponse;
    const trimmedBody = typeof body === 'string' ? body.trim() : '';

    // If status is okay but custom check failed, log that reason only
    if (isExpectedStatusMatches && customReason) {
        console.error(`❌ ${checkLabel} failed: Custom validation failed. Reason: ${customReason}. URL: ${url}`);
        return;
    }

    // Categorize status
    let errorCategory;
    if (status >= 400 && status < 500) {
        errorCategory = 'Client Error (4xx)';
    } else if (status >= 500 && status < 600) {
        errorCategory = 'Server Error (5xx)';
    } else if (status === 0) {
        errorCategory = 'Network Error or Timeout (status = 0)';
    } else {
        errorCategory = `Unexpected Status (${status})`;
    }

    if (!trimmedBody) {
        console.error(`❌ ${checkLabel} failed: ${errorCategory}. Empty response body. URL: ${url}`);
        return;
    }

    try {
        const responseJson = JSON.parse(trimmedBody);
        const errorMessage = responseJson && responseJson.message ? responseJson.message : 'No message';
        const errorDetails = responseJson && responseJson.details ? responseJson.details : 'No details';
        console.error(`❌ ${checkLabel} failed: ${errorCategory}. Status: ${status}, Message: ${errorMessage}, Details: ${errorDetails}, URL: ${url}`);
    } catch (e) {
        const bodyPreview = trimmedBody.length > 500 ? trimmedBody.slice(0, 500) + '... [truncated]' : trimmedBody;
        console.error(`❌ ${checkLabel} failed: ${errorCategory}. Status: ${status}, URL: ${url}. Response is not valid JSON.\n↪️ Raw body preview:\n${bodyPreview}\n✳️ Parse error: ${e.message}`);
    }
}