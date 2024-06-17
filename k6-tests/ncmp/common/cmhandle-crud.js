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
import { check, sleep, fail } from 'k6';
import { NCMP_BASE_URL, DMI_PLUGIN_URL, TOTAL_CM_HANDLES } from './utils.js';

export function createCmHandles(cmHandleIds) {
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

export function deleteCmHandles(cmHandleIds) {
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

export function waitForCmHandlesToBeReady(timeOutInSeconds) {
    const pollingIntervalInSeconds = 10;
    const maxRetries = Math.ceil(timeOutInSeconds / pollingIntervalInSeconds);
    let cmHandlesReady = 0;
    for (let currentTry = 0; currentTry <= maxRetries; currentTry++) {
        sleep(pollingIntervalInSeconds);
        try {
            cmHandlesReady = getNumberOfReadyCmHandles();
        } catch (error) {
            console.error(`Attempt ${currentTry + 1} - Error fetching CM handles: ${error.message}`);
        }
        console.log(`Attempt ${currentTry + 1} - ${cmHandlesReady}/${TOTAL_CM_HANDLES} CM handles are READY`);
        if (cmHandlesReady === TOTAL_CM_HANDLES) {
            console.log(`All ${TOTAL_CM_HANDLES} CM handles are READY`);
            return;
        }
    }
    fail(`Timed out after ${timeOutInSeconds} seconds waiting for ${TOTAL_CM_HANDLES} CM handles to be READY`);
}

function getNumberOfReadyCmHandles() {
    const endpointUrl = `${NCMP_BASE_URL}/cps/api/v2/dataspaces/NCMP-Admin/anchors/ncmp-dmi-registry/node?xpath=/dmi-registry&descendants=all`;
    const jsonData = http.get(endpointUrl).json();
    const cmHandles = jsonData[0]["dmi-reg:dmi-registry"]["cm-handles"];
    return cmHandles.filter(cmhandle => cmhandle['state']['cm-handle-state'] === 'READY').length;
}
