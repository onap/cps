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

import { sleep } from 'k6';
import { performPostRequest, NCMP_BASE_URL, DMI_PLUGIN_URL, TOTAL_CM_HANDLES, MODULE_SET_TAGS
} from './utils.js';
import { executeCmHandleIdSearch } from './search-base.js';

export function createCmHandles(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmpInventory/v1/ch`;
    const payload = JSON.stringify(createCmHandlePayload(cmHandleIds));
    return performPostRequest(url, payload, 'createCmHandles');
}

export function deleteCmHandles(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmpInventory/v1/ch`;
    const payload = JSON.stringify({
        "dmiPlugin": DMI_PLUGIN_URL,
        "removedCmHandles": cmHandleIds,
    });
    return performPostRequest(url, payload, 'deleteCmHandles');
}

export function waitForAllCmHandlesToBeReady() {
    const POLLING_INTERVAL_SECONDS = 5;
    let cmHandlesReady = 0;
    do {
        sleep(POLLING_INTERVAL_SECONDS);
        cmHandlesReady = getNumberOfReadyCmHandles();
        console.log(`${cmHandlesReady}/${TOTAL_CM_HANDLES} CM handles are READY`);
    } while (cmHandlesReady < TOTAL_CM_HANDLES);
}

function createCmHandlePayload(cmHandleIds) {
    return {
        "dmiPlugin": DMI_PLUGIN_URL,
        "createdCmHandles": cmHandleIds.map((cmHandleId, index) => ({
            "cmHandle": cmHandleId,
            "alternateId": cmHandleId.replace('ch-', 'Subnetwork=Europe,ManagedElement='),
            "moduleSetTag": MODULE_SET_TAGS[index % MODULE_SET_TAGS.length],
            "cmHandleProperties": {
                "id": "123",
                "attributes": {"userLabel": "test"}
            },
            "publicCmHandleProperties": {
                "Color": "yellow",
                "Size": "small",
                "Shape": "cube"
            }
        })),
    };
}

function getNumberOfReadyCmHandles() {
    const response = executeCmHandleIdSearch('cps-path-for-ready-cm-handles');
    const arrayOfCmHandleIds = JSON.parse(response.body);
    return arrayOfCmHandleIds.length;
}
