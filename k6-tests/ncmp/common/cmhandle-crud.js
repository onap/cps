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
import { check, sleep } from 'k6';
import { NCMP_BASE_URL, DMI_PLUGIN_URL, TOTAL_CM_HANDLES, REGISTRATION_BATCH_SIZE, makeBatchOfCmHandleIds } from './utils.js';

export function registerAllCmHandles() {
    const TOTAL_BATCHES = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < TOTAL_BATCHES; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        createCmHandles(nextBatchOfCmHandleIds);
    }
    waitForCmHandlesToBeReady();
}

export function deregisterAllCmHandles() {
    const TOTAL_BATCHES = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < TOTAL_BATCHES; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        deleteCmHandles(nextBatchOfCmHandleIds);
    }
}

function createCmHandles(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmpInventory/v1/ch`;
    const payload = {
        "dmiPlugin": DMI_PLUGIN_URL,
        "createdCmHandles": cmHandleIds.map(cmHandleId => ({
            "cmHandle": cmHandleId,
            "cmHandleProperties": {"neType": "RadioNode"},
            "publicCmHandleProperties": {
                "Color": "yellow",
                "Size": "small",
                "Shape": "cube"
            }
        })),
    };
    const params = {
        headers: {'Content-Type': 'application/json'}
    };
    const response = http.post(url, JSON.stringify(payload), params);
    check(response, {
        'status equals 200': (r) => r.status === 200,
    });
    return response;
}

function deleteCmHandles(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmpInventory/v1/ch`;
    const payload = {
        "dmiPlugin": DMI_PLUGIN_URL,
        "removedCmHandles": cmHandleIds,
    };
    const params = {
        headers: {'Content-Type': 'application/json'}
    };
    const response = http.post(url, JSON.stringify(payload), params);
    check(response, {
        'status equals 200': (r) => r.status === 200,
    });
    return response;
}

function waitForCmHandlesToBeReady() {
    const pollingIntervalInSeconds = 5;
    let cmHandlesReady = 0;
    do {
        sleep(pollingIntervalInSeconds);
        cmHandlesReady = getNumberOfReadyCmHandles();
        console.log(`${cmHandlesReady}/${TOTAL_CM_HANDLES} CM handles are READY`);
    } while (cmHandlesReady < TOTAL_CM_HANDLES);
}

function getNumberOfReadyCmHandles() {
    const url = `${NCMP_BASE_URL}/ncmp/v1/ch/id-searches`;
    const params = {
        headers: {'Content-Type': 'application/json'}
    };
    const searchParameters = {
        "cmHandleQueryParameters": [
            {
                "conditionName": "cmHandleWithCpsPath",
                "conditionParameters": [{"cpsPath": "//state[@cm-handle-state='READY']"}]
            }
        ]
    };
    const payload = JSON.stringify(searchParameters);
    const response = http.post(url, payload, params);
    const arrayOfCmHandleIds = JSON.parse(response.body);
    return arrayOfCmHandleIds.length;
}
