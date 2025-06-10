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
 * Validates a response against an expected HTTP status and an optional additional check.
 * If successful, records a metric value to a trend. Logs detailed error output on failure.
 *
 * @param {Object} response - The HTTP response object from a K6 request.
 * @param {number} expectedStatus - The expected HTTP status code (e.g., 200, 202).
 * @param {string} checkLabel - The label to use in the K6 `check()` for reporting.
 * @param {Trend} trendMetric - A K6 `Trend` metric to which the extracted value will be added on success.
 * @param {function(Object): number} metricExtractor - A function that takes the response and returns a numeric value to record (e.g., `res.timings.duration`).
 * @param {function(Object): boolean} [additionalCheckValidator=() => true] - Optional function for any additional custom validation on the response.
 *
 */
export function validateAndRecordMetric(response, expectedStatus, checkLabel, trendMetric, metricExtractor, additionalCheckValidator = () => true) {

    const isExpectedStatus = response.status === expectedStatus;
    const isAdditionalCheckValid = additionalCheckValidator(response);
    const isSuccess = check(response, {
        [checkLabel]: () => isExpectedStatus && isAdditionalCheckValid,
    });

    if (isSuccess) {
        trendMetric.add(metricExtractor(response));
    } else {
        if (!isExpectedStatus) {
            try {
                const responseBody = JSON.parse(response.body);
                console.error(`❌ ${checkLabel} failed: Error response status: ${response.status}, message: ${responseBody.message}, details: ${responseBody.details}`);
            } catch (e) {
                console.error(`❌ ${checkLabel} failed: Unable to parse response body.`);
            }
        }
    }
}

export function processHttpResponseWithOverheadMetrics(response, expectedStatus, checkLabel, delayMs, trendMetric) {
    validateAndRecordMetric(response, expectedStatus, checkLabel, trendMetric, (res) => res.timings.duration - delayMs);
}

export function validateResponseAndRecordMetric(response, expectedStatus, expectedJsonLength, trendMetric, checkLabel) {
    validateAndRecordMetric(response, expectedStatus, checkLabel, trendMetric, (res) => res.timings.duration, (res) => res.json('#') === expectedJsonLength);
}

