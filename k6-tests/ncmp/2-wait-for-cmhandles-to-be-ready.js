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
import { sleep, fail } from 'k6';
import { makeCustomSummaryReport, NCMP_BASE_URL, TOTAL_CM_HANDLES } from './utils.js';

export const options = {
    vus: 1,
    iterations: 1,
    thresholds: {
        http_req_failed: ['rate == 0'],
        iteration_duration: ['max <= 260000'], // 4m20s
    },
};

export default function () {
    waitForCmHandlesToBeReady(TOTAL_CM_HANDLES);
}

function waitForCmHandlesToBeReady(totalCmHandles) {
    const timeOutInSeconds = 6 * 60;
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
        console.log(`Attempt ${currentTry + 1} - ${cmHandlesReady}/${totalCmHandles} CM handles are READY`);
        if (cmHandlesReady === totalCmHandles) {
            console.log(`All ${totalCmHandles} CM handles are READY`);
            return;
        }
    }
    fail(`Timed out after ${timeoutInSeconds} seconds waiting for ${totalCmHandles} CM handles to be READY`);
}

function getNumberOfReadyCmHandles() {
    const endpointUrl = `${NCMP_BASE_URL}/cps/api/v2/dataspaces/NCMP-Admin/anchors/ncmp-dmi-registry/node?xpath=/dmi-registry&descendants=all`;
    const jsonData = http.get(endpointUrl).json();
    const cmHandles = jsonData[0]["dmi-reg:dmi-registry"]["cm-handles"];
    return cmHandles.filter(cmhandle => cmhandle['state']['cm-handle-state'] === 'READY').length;
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
