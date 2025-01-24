/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

export const testConfig = JSON.parse(open(`../config/${__ENV.TEST_PROFILE}.json`));
export const KAFKA_BOOTSTRAP_SERVERS = testConfig.hosts.kafkaBootstrapServer;
export const NCMP_BASE_URL = testConfig.hosts.ncmpBaseUrl;
export const DMI_PLUGIN_URL = testConfig.hosts.dmiStubUrl;
export const CONTAINER_UP_TIME_IN_SECONDS = testConfig.hosts.containerUpTimeInSeconds;
export const LEGACY_BATCH_TOPIC_NAME = 'legacy_batch_topic';
export const TOTAL_CM_HANDLES = 50000;
export const REGISTRATION_BATCH_SIZE = 100;
export const READ_DATA_FOR_CM_HANDLE_DELAY_MS = 300; // must have same value as in docker-compose.yml
export const WRITE_DATA_FOR_CM_HANDLE_DELAY_MS = 670; // must have same value as in docker-compose.yml
export const CONTENT_TYPE_JSON_PARAM = {'Content-Type': 'application/json'};
export const LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE = 200;
export const LEGACY_BATCH_THROUGHPUT_TEST_NUMBER_OF_REQUESTS = 100;
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
        makeSummaryCsvLine('0', 'HTTP request failures for all tests', 'rate of failed requests', 'http_req_failed', 0, testResults, scenarioConfig),
        makeSummaryCsvLine('1', 'Registration of CM-handles', 'CM-handles/second', 'cmhandles_created_per_second', 90, testResults, scenarioConfig),
        makeSummaryCsvLine('2', 'De-registration of CM-handles', 'CM-handles/second', 'cmhandles_deleted_per_second', 140, testResults, scenarioConfig),
        makeSummaryCsvLine('3a', 'CM-handle ID search with No filter', 'milliseconds', 'id_search_nofilter_duration', 300, testResults, scenarioConfig),
        makeSummaryCsvLine('3b', 'CM-handle ID search with Module filter', 'milliseconds', 'id_search_module_duration', 120, testResults, scenarioConfig),
        makeSummaryCsvLine('3c', 'CM-handle ID search with Property filter', 'milliseconds', 'id_search_property_duration', 750, testResults, scenarioConfig),
        makeSummaryCsvLine('3d', 'CM-handle ID search with Cps Path filter', 'milliseconds', 'id_search_cpspath_duration', 750, testResults, scenarioConfig),
        makeSummaryCsvLine('3e', 'CM-handle ID search with Trust Level filter', 'milliseconds', 'id_search_trustlevel_duration', 3000, testResults, scenarioConfig),
        makeSummaryCsvLine('4a', 'CM-handle search with No filter', 'milliseconds', 'cm_search_nofilter_duration', 3000, testResults, scenarioConfig),
        makeSummaryCsvLine('4b', 'CM-handle search with Module filter', 'milliseconds', 'cm_search_module_duration', 4000, testResults, scenarioConfig),
        makeSummaryCsvLine('4c', 'CM-handle search with Property filter', 'milliseconds', 'cm_search_property_duration', 4500, testResults, scenarioConfig),
        makeSummaryCsvLine('4d', 'CM-handle search with Cps Path filter', 'milliseconds', 'cm_search_cpspath_duration', 4500, testResults, scenarioConfig),
        makeSummaryCsvLine('4e', 'CM-handle search with Trust Level filter', 'milliseconds', 'cm_search_trustlevel_duration', 7000, testResults, scenarioConfig),
        makeSummaryCsvLine('5a', 'NCMP overhead for Synchronous single CM-handle pass-through read', 'milliseconds', 'ncmp_overhead_passthrough_read', 20, testResults, scenarioConfig),
        makeSummaryCsvLine('5b', 'NCMP overhead for Synchronous single CM-handle pass-through read with alternate id', 'milliseconds', 'ncmp_overhead_passthrough_read_alt_id', 40, testResults, scenarioConfig),
        makeSummaryCsvLine('6a', 'NCMP overhead for Synchronous single CM-handle pass-through write', 'milliseconds', 'ncmp_overhead_passthrough_write', 20, testResults, scenarioConfig),
        makeSummaryCsvLine('6b', 'NCMP overhead for Synchronous single CM-handle pass-through write with alternate id', 'milliseconds', 'ncmp_overhead_passthrough_write_alt_id', 40, testResults, scenarioConfig),
        makeSummaryCsvLine('7', 'Legacy batch read operation', 'events/second', 'legacy_batch_read_cmhandles_per_second', 300, testResults, scenarioConfig),
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
