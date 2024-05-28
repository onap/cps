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
import exec from 'k6/execution';
import { check } from 'k6';
import {
    NCMP_BASE_URL,
    DMI_PLUGIN_URL,
    TOTAL_CM_HANDLES,
    makeBatchOfCmHandleIds,
    makeCustomSummaryReport
} from './utils.js';

const BATCH_SIZE = 100;
export const options = {
    vus: 1,
    iterations: Math.ceil(TOTAL_CM_HANDLES / BATCH_SIZE),
    thresholds: {
        http_req_failed: ['rate == 0'],
        http_req_duration: ['avg <= 1000'],
    },
};

export default function () {
    const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(BATCH_SIZE, exec.scenario.iterationInTest);
    const payload = {
        "dmiPlugin": DMI_PLUGIN_URL,
        "createdCmHandles": nextBatchOfCmHandleIds.map(cmHandleId => ({
            "cmHandle": cmHandleId,
            "cmHandleProperties": {"neType": "RadioNode"},
            "publicCmHandleProperties": {
                "Color": "yellow",
                "Size": "small",
                "Shape": "cube"
            }
        })),
    };
    const response = http.post(NCMP_BASE_URL + '/ncmpInventory/v1/ch', JSON.stringify(payload), {
        headers: {'Content-Type': 'application/json'},
    });
    check(response, {
        'status equals 200': (r) => r.status === 200,
    });
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}