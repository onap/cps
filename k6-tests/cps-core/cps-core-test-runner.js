/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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
import { testConfig, makeCustomSummaryReport } from './common/utils.js';

const trendNamesFileContent = open('./config/trendNames.txt').trim();
const trendNames = trendNamesFileContent ? trendNamesFileContent.split('\n') : [];
const kpiTrendDeclarations = {};

for (const trendName of trendNames) {
    kpiTrendDeclarations[trendName] = new Trend(trendName, true);
}

export const options = {
    setupTimeout: '30m',
    teardownTimeout: '20m',
    scenarios: testConfig.scenarios,
    thresholds: testConfig.thresholds,
};

export function setup() {
}

export function teardown() {
}

// No-op default: keeps k6 happy while `scenarios` is empty (no scenarios wired up yet).
// Remove once a scenario is added to config/kpi.json + config/endurance.json.
export default function () {
}

export function handleSummary(data) {
    const testProfile = __ENV.TEST_PROFILE;
    if (testProfile === 'kpi') {
        console.log("✅ Generating KPI summary...");
        return {
            stdout: makeCustomSummaryReport(data, options),
        };
    }
    console.log("⛔ Skipping KPI summary (not in 'kpi' profile)");
    return {};
}
