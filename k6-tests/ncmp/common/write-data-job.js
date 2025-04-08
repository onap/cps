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
 * Main entry point for executing write data jobs based on scenario parameters.
 * For 'large' dataSize: Single request with 10,000 operations.
 * For 'small' dataSize: 10 parallel requests with 100 operations each.
 *
 * @param {Object} scenarioParams - Parameters defining the scenario.
 */
export async function executeWriteDataJob(scenarioParams) {
    if (scenarioParams.dataSize === "large") {
        console.log("Executing large write data job request...");

        const dataJobId = generateDataJobId();
        const numberOfWriteOperations = 10_000;

        const dataJobRequest = populateDataJobWriteRequests(numberOfWriteOperations);
        return executeWriteDataJobRequest(dataJobId, dataJobRequest);

    } else {
        console.log("Executing small write data Job with 10 parallel operations...");
        return executeParallelWriteOperations(100); // Each with 100 write operations
    }
}

/**
 * Executes 10 parallel write operations with unique dataJobIds.
 * Each job contains `numberOfWriteOperations` operations.
 *
 * @param {number} numberOfWriteOperations - Number of operations per data job.
 * @returns {Promise[]} Array of promises for all write operations.
 */
export async function executeParallelWriteOperations(numberOfWriteOperations) {
    const numberOfParallelExecution = 10;

    const writeOperations = Array.from({ length: numberOfParallelExecution }, () => {
        const dataJobId = generateDataJobId();
        const dataJobRequest = populateDataJobWriteRequests(numberOfWriteOperations);
        return executeWriteDataJobRequest(dataJobId, dataJobRequest);
    });

    try {
        const responses = await Promise.all(writeOperations);
        console.log('All small write operations completed successfully.');
        return responses;
    } catch (error) {
        console.error('An error occurred during parallel write operations:', error);
        throw error;
    }
}

/**
 * Executes a write data job request.
 *
 * @param {string} dataJobId - Unique data job ID.
 * @param {Object} dataJobRequest - Data job request payload.
 * @returns {Promise} A promise that resolves with the response of the POST request.
 */
function executeWriteDataJobRequest(dataJobId, dataJobRequest) {
    const payload = JSON.stringify(dataJobRequest);
    const urlOnHiddenEndPoint = `${NCMP_BASE_URL}/ncmp/v1/do-not-use/dataJobs/${dataJobId}/write`;
    return performPostRequest(urlOnHiddenEndPoint, payload, 'WriteDataJob');
}

/**
 * Generates a unique data job ID using crypto.randomUUID().
 *
 * @returns {string} A unique data job ID.
 */
function generateDataJobId() {
    return crypto.randomUUID();
}

/**
 * Generates a write request payload with a given number of operations.
 *
 * @param {number} numberOfWriteOperations - Number of base paths to create operations for.
 * @returns {Object} Data job request payload.
 */
function populateDataJobWriteRequests(numberOfWriteOperations) {
    const writeOperations = [];

    for (let i = 1; i <= numberOfWriteOperations; i++) {
        const basePath = `/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${i}/ManagedElement=MyManagedElement${i}`;

        writeOperations.push(
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
    return { data: writeOperations };
}


