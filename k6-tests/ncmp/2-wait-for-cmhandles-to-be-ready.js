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

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate==0'],
    iteration_duration: ['max <= 300_000'], // 5 minutes
  },
};

export default function() {
    waitForCmHandlesToBeReady(20000);
}

function waitForCmHandlesToBeReady(totalCmHandles) {
    const pollingIntervalInSeconds = 5;
    let cmHandlesReady = 0;
    while (cmHandlesReady < totalCmHandles) {
        sleep(pollingIntervalInSeconds);
        try {
            cmHandlesReady = getNumberOfReadyCmHandles();
        } catch (e) {
            console.log(`Error: ${e}`);
        }
        console.log(`${cmHandlesReady} CM-handles are READY`);
    }
}

function getNumberOfReadyCmHandles() {
    let cmHandlesReady = 0;
    const endpointUrl = 'http://localhost:8883/cps/api/v2/dataspaces/NCMP-Admin/anchors/ncmp-dmi-registry/node?xpath=/dmi-registry&descendants=all';
    const jsonData = http.get(endpointUrl).json();
    for (let cmhandle of jsonData[0]["dmi-reg:dmi-registry"]["cm-handles"]) {
        if (cmhandle['state']['cm-handle-state'] == 'READY') {
            cmHandlesReady++;
        }
    }
    return cmHandlesReady;
}
