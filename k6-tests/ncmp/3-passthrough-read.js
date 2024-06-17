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

import { Trend } from 'k6/metrics';
import { passthroughRead } from './common/passthrough-read.js'
import { makeCustomSummaryReport } from './common/utils.js'

let ncmpOverheadTrend = new Trend("ncmp_overhead");

export const options = {
    vus: 12,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate == 0'],
        ncmp_overhead: ['avg <= 50'],
    },
};

// The function that defines VU logic.
export default function () {
    const response = passthroughRead();
    // Calculate overhead assuming DMI data delay is 2500ms.
    const dmiDelay = 2500; // This should be same as value DATA_FOR_CM_HANDLE_DELAY_MS in docker-compose.yml
    const overhead = response.timings.duration - dmiDelay;
    ncmpOverheadTrend.add(overhead);
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
