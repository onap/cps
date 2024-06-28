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
export const TOTAL_CM_HANDLES = Number(__ENV.TOTAL_CM_HANDLES) || 20000;
export const REGISTRATION_BATCH_SIZE = Number(__ENV.REGISTRATION_BATCH_SIZE) || 100;

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
        let cmHandleId = 'ch-' + (startIndex + i);
        batchOfIds.push(cmHandleId);
    }
    return batchOfIds;
}

export function getRandomCmHandleId() {
    return `ch-${Math.floor(Math.random() * TOTAL_CM_HANDLES) + 1}`;
}

export function makeCustomSummaryReport(data, options) {
    let summaryCsv = 'Test Case,Unit,Limit,Actual\n';
    summaryCsv += makeSummaryCsvLine('1. Registration of CM-handles', 'CM-handles/second', 'cmhandles_created_per_second', data, options);
    summaryCsv += makeSummaryCsvLine('2. De-registration of CM-handles', 'CM-handles/second', 'cmhandles_deleted_per_second', data, options);
    summaryCsv += makeSummaryCsvLine('3. CM-handle ID search with Module filter', 'milliseconds', 'http_req_duration{scenario:id_search_module}', data, options);
    summaryCsv += makeSummaryCsvLine('4. CM-handle search with Module filter', 'milliseconds', 'http_req_duration{scenario:cm_search_module}', data, options);
    summaryCsv += makeSummaryCsvLine('5. Synchronous single CM-handle pass-through read', 'milliseconds', 'http_req_duration{scenario:passthrough_read}', data, options);
    return summaryCsv;
}

function makeSummaryCsvLine(testCase, unit, thresholdInK6, data, options) {
    const thresholdArray = JSON.parse(JSON.stringify(options.thresholds[thresholdInK6]));
    const thresholdString = thresholdArray[0];
    const [thresholdKey, thresholdOperator, thresholdValue] = thresholdString.split(/\s+/);
    const actualValue = data.metrics[thresholdInK6].values[thresholdKey].toFixed(3);
    return `${testCase},${unit},${thresholdValue},${actualValue}\n`;
}
