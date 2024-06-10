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

import exec from 'k6/execution';
import { TOTAL_CM_HANDLES, makeBatchOfCmHandleIds, makeCustomSummaryReport } from './common/utils.js';
import { deleteCmHandles } from './common/cmhandle-registration.js';

const BATCH_SIZE = 100;
export const options = {
    vus: 1,
    iterations: Math.ceil(TOTAL_CM_HANDLES / BATCH_SIZE),
    thresholds: {
        http_req_failed: ['rate == 0'],
        http_req_duration: ['avg <= 1050'],
    },
};

export default function () {
    const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(BATCH_SIZE, exec.scenario.iterationInTest);
    deleteCmHandles(nextBatchOfCmHandleIds);
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
