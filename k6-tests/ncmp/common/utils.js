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

export const NCMP_BASE_URL = 'http://localhost:8883';
export const DMI_PLUGIN_URL = 'http://ncmp-dmi-plugin-demo-and-csit-stub:8092';
export const TOTAL_CM_HANDLES = 20000;
export const REGISTRATION_BATCH_SIZE = 100;
export const READ_DATA_FOR_CM_HANDLE_DELAY_MS = 300; // must have same value as in docker-compose.yml
export const WRITE_DATA_FOR_CM_HANDLE_DELAY_MS = 670; // must have same value as in docker-compose.yml
export const CONTENT_TYPE_JSON_PARAM = { headers: {'Content-Type': 'application/json'} };
export const DATA_OPERATION_READ_BATCH_SIZE = 200;
export const TOPIC_DATA_OPERATIONS_BATCH_READ = 'topic-data-operations-batch-read';
export const KAFKA_BOOTSTRAP_SERVERS = ['localhost:9092'];

export function recordTimeInSeconds(functionToExecute) {
    const startTimeInMillis = Date.now();
    functionToExecute();
    const endTimeInMillis = Date.now();
    const totalTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;
    return totalTimeInSeconds;
}

/**
 * Generates a batch of CM-handle IDs based on batch size and number.
 * @param {number} batchSize - Size of each batch.
 * @param {number} batchNumber - Number of the batch.
 * @returns {string[]} Array of CM-handle IDs, for example ['ch-201', 'ch-202' ... 'ch-300']
 */
export function makeBatchOfCmHandleIds(batchSize, batchNumber) {
    const batchOfIds = [];
    const startIndex = 1 + batchNumber * batchSize;
    for (let i = 0; i < batchSize; i++) {
        let cmHandleId = `ch-${startIndex + i}`;
        batchOfIds.push(cmHandleId);
    }
    return batchOfIds;
}

export function getRandomCmHandleId() {
    return `ch-${Math.floor(Math.random() * TOTAL_CM_HANDLES) + 1}`;
}

export function makeCustomSummaryReport(data, options) {
    const summaryCsvLines = [
        '#,Test Name,Unit,Limit,Actual',
        makeSummaryCsvLine('1', 'Registration of CM-handles', 'CM-handles/second', 'cmhandles_created_per_second', data, options),
        makeSummaryCsvLine('2', 'De-registration of CM-handles', 'CM-handles/second', 'cmhandles_deleted_per_second', data, options),
        makeSummaryCsvLine('3', 'CM-handle ID search with Module filter', 'milliseconds', 'http_req_duration{scenario:id_search_module}', data, options),
        makeSummaryCsvLine('4', 'CM-handle search with Module filter', 'milliseconds', 'http_req_duration{scenario:cm_search_module}', data, options),
        makeSummaryCsvLine('5a', 'Synchronous single CM-handle pass-through read', 'requests/second', 'http_reqs{scenario:passthrough_read}', data, options),
        makeSummaryCsvLine('5b', 'NCMP overhead for Synchronous single CM-handle pass-through read', 'milliseconds', 'ncmp_overhead_passthrough_read', data, options),
        makeSummaryCsvLine('6a', 'Synchronous single CM-handle pass-through write', 'requests/second', 'http_reqs{scenario:passthrough_write}', data, options),
        makeSummaryCsvLine('6b', 'NCMP overhead for Synchronous single CM-handle pass-through write', 'milliseconds', 'ncmp_overhead_passthrough_write', data, options),
        makeSummaryCsvLine('7', 'Data operations batch read', 'CM-handles/seconds', 'data_operations_batch_read_cmhandles_per_second', data, options),
    ];
    return summaryCsvLines.join('\n') + '\n';
}

function makeSummaryCsvLine(testCase, testName, unit, thresholdInK6, data, options) {
    const thresholdArray = JSON.parse(JSON.stringify(options.thresholds[thresholdInK6]));
    const thresholdString = thresholdArray[0];
    const [thresholdKey, thresholdOperator, thresholdValue] = thresholdString.split(/\s+/);
    const actualValue = data.metrics[thresholdInK6].values[thresholdKey].toFixed(3);
    return `${testCase},${testName},${unit},${thresholdValue},${actualValue}`;
}
