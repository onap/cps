/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import {performPostRequest, NCMP_BASE_URL} from './utils.js';

/**
 * Main function to execute a write data job with operations count based on dataSize.
 *
 * - 'large' → 10,000 operations
 * - 'small' → 100 operations
 *
 * @param {Object} writeDataJobParam - Scenario configuration.
 * @returns {*} Response from the POST request.
 */
export function executeWriteDataJob(writeDataJobParam) {
    const numberOfOperations = writeDataJobParam.dataSize === "large" ? 10000 : 100;
    const jobId = generateUniqueJobId();
    const requestPayload = buildWriteOperationPayload(numberOfOperations);

    console.log(`[WriteJob] Starting job → ID: ${jobId}, Operations: ${numberOfOperations}`);
    return sendWriteDataJobRequest(jobId, requestPayload);
}

/**
 * Sends a write data job request to the NCMP endpoint.
 *
 * @param {string} jobId - Unique job ID.
 * @param {Object} payload - Write request body.
 * @returns {*} Response from the POST request.
 */
function sendWriteDataJobRequest(jobId, payload) {
    const targetUrl = `${NCMP_BASE_URL}/ncmp/v1/do-not-use/dataJobs/${jobId}/write`;
    const serializedPayload = JSON.stringify(payload);
    return performPostRequest(targetUrl, serializedPayload, 'WriteDataJob');
}

/**
 * Generates a unique job ID using crypto.randomUUID().
 *
 * @returns {string} Unique job identifier.
 */
function generateUniqueJobId() {
    return crypto.randomUUID();
}

/**
 * Builds the write request payload for a given number of base operation sets.
 *
 * Each base operation set includes: add, merge, remove.
 *
 * @param {number} numberOfWriteOperations - Number of base sets to generate.
 * @returns {Object} Payload with all write operations.
 */
function buildWriteOperationPayload(numberOfWriteOperations) {
    const operations = [];

    for (let i = 1; i <= numberOfWriteOperations; i++) {
        const basePath = `/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${i}/ManagedElement=MyManagedElement${i}`;

        operations.push(
            {
                path: `${basePath}/SomeChild=child-1`,
                op: 'add',
                operationId: `${i}-1`,
                value: 'some-value-one'
            },
            {
                path: `${basePath}/SomeChild=child-2`,
                op: 'merge',
                operationId: `${i}-2`,
                value: 'some-value-two'
            },
            {
                path: basePath,
                op: 'remove',
                operationId: `${i}-3`,
                value: 'some-value-three'
            }
        );
    }

    return {data: operations};
}


