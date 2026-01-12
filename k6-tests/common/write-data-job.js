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

import {performPostRequest, getRandomAlternateId, NCMP_BASE_URL} from './utils.js';

/**
 * Executes a write data job against the NCMP endpoint.
 *
 * @param {number} numberOfOperations - Number of base operation sets to include in the job.
 * @returns {*} The HTTP response from the POST request.
 */
export function executeWriteDataJob(numberOfOperations) {
    const jobId = crypto.randomUUID();
    const requestPayload = buildDataJobRequestPayload(numberOfOperations);

    console.debug(`[WriteJob] Starting job → ID: ${jobId}, Operations: ${numberOfOperations}`);
    return sendWriteDataJobRequest(jobId, requestPayload);
}

/**
 * Sends a write data job request to the NCMP API endpoint.
 *
 * @param {string} jobId - The unique identifier for this write job.
 * @param {Object} payload - The complete request body for the write operation.
 * @returns {*} The response from the HTTP POST request.
 */
function sendWriteDataJobRequest(jobId, payload) {
    const targetUrl = `${NCMP_BASE_URL}/do-not-use/dataJobs/${jobId}/write`;
    const serializedPayload = JSON.stringify(payload);
    return performPostRequest(targetUrl, serializedPayload, 'WriteDataJob');
}

/**
 * Builds the full payload for a write data job.
 *
 * Each base operation set consists of three write operations:
 *  - `add` at a nested child path
 *  - `merge` at a different child path
 *  - `remove` at the parent path
 *
 * The structure returned matches the expected `DataJobRequest` format on the server side:
 *
 * Java-side representation:
 * ```java
 * public record DataJobRequest(
 *   DataJobMetadata dataJobMetadata,
 *   DataJobWriteRequest dataJobWriteRequest
 * )
 * ```
 *
 * @param {number} numberOfWriteOperations - The number of base sets to generate (each set = 3 operations).
 * @returns {{
 *   dataJobMetadata: {
 *     destination: string,
 *     dataAcceptType: string,
 *     dataContentType: string
 *   },
 *   dataJobWriteRequest: {
 *     data: Array<{
 *       path: string,
 *       op: "add" | "merge" | "remove",
 *       operationId: string,
 *       value: Map<String, Object>
 *     }>
 *   }
 * }} Fully-formed data job request payload.
 */
function buildDataJobRequestPayload(numberOfWriteOperations) {
    const operations = [];
    for (let i = 1; i <= numberOfWriteOperations / 2; i++) {
        const basePath = getRandomAlternateId();
        operations.push(
            {
                path: `${basePath}/SomeChild=child-1`,
                op: 'add',
                operationId: `${i}-1`,
                value: {
                    key: `some-value-one-${i}`
                }
            },
            {
                path: `${basePath}/SomeChild=child-2/SomeGrandChild=grand-child-2`,
                op: 'merge',
                operationId: `${i}-2`,
                value: {
                    key: `some-value-two-${i}`
                }
            }
        );
    }
    return {
        dataJobMetadata: {
            destination: "device/managed-element-collection",
            dataAcceptType: "application/json",
            dataContentType: "application/merge-patch+json"
        },
        dataJobWriteRequest: {
            data: operations
        }
    };
}