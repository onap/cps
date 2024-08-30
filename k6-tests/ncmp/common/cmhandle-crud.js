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
import { sleep } from 'k6';
import {
    NCMP_BASE_URL, DMI_PLUGIN_URL, TOTAL_CM_HANDLES,
    MODULE_SET_TAGS, CONTENT_TYPE_JSON_PARAM
} from './utils.js';
import { executeCmHandleIdSearch } from './search-base.js';

// Helper function to perform POST requests with JSON payload and content type
function performPostRequest(url, payload, metricTag) {
    const metricTags = {endpoint: metricTag};

    return http.post(url, JSON.stringify(payload), {
        headers: CONTENT_TYPE_JSON_PARAM,
        tags: metricTags
    });
}

// Helper function to wait for a condition to be met with polling
function checkIfCmHandlesAreReady(checkFunction, targetCmHandleCount, pollingIntervalSeconds) {
    let cmHandlesReady = 0;
    do {
        sleep(pollingIntervalSeconds);
        cmHandlesReady = checkFunction();
        console.log(`${cmHandlesReady}/${targetCmHandleCount} CM handles are READY`);
    } while (cmHandlesReady < targetCmHandleCount);
}

// Helper function to create payload for CM handle creation
function createCmHandlePayload(cmHandleIds) {
    return {
        "dmiPlugin": DMI_PLUGIN_URL,
        "createdCmHandles": cmHandleIds.map((cmHandleId, index) => ({
            "cmHandle": cmHandleId,
            "alternateId": `alt-${cmHandleId}`,
            "moduleSetTag": MODULE_SET_TAGS[index % MODULE_SET_TAGS.length],
            "cmHandleProperties": {"neType": "RadioNode"},
            "publicCmHandleProperties": {
                "Color": "yellow",
                "Size": "small",
                "Shape": "cube"
            }
        })),
    };
}

export function createCmHandles(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmpInventory/v1/ch`;
    const payload = createCmHandlePayload(cmHandleIds);
    return performPostRequest(url, payload, 'createCmHandles');
}

export function deleteCmHandles(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmpInventory/v1/ch`;
    const payload = {
        "dmiPlugin": DMI_PLUGIN_URL,
        "removedCmHandles": cmHandleIds,
    };
    return performPostRequest(url, payload, 'deleteCmHandles');
}

export function waitForAllCmHandlesToBeReady() {
    const POLLING_INTERVAL_SECONDS = 5;
    checkIfCmHandlesAreReady(getNumberOfReadyCmHandles, TOTAL_CM_HANDLES, POLLING_INTERVAL_SECONDS);
}

function getNumberOfReadyCmHandles() {
    const response = executeCmHandleIdSearch('readyCmHandles');
    const arrayOfCmHandleIds = JSON.parse(response.body);
    return arrayOfCmHandleIds.length;
}