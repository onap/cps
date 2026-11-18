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

import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { testConfig, makeCustomSummaryReport, CONTAINER_COOL_DOWN_TIME_IN_SECONDS } from './common/utils.js';
import {
    createDataspace, deleteDataspace,
    createSchemaSet, deleteSchemaSet,
    createAnchor, deleteAnchor
} from './common/admin-crud.js';

const trendNamesFileContent = open('./config/trendNames.txt').trim();
const trendNames = trendNamesFileContent ? trendNamesFileContent.split('\n') : [];
const kpiTrendDeclarations = {};

for (const trendName of trendNames) {
    kpiTrendDeclarations[trendName] = new Trend(trendName, true);
}

const DATASPACE_NAME = 'k6PerfDataspace';
const SCHEMA_SET_NAME = 'k6PerfSchemaSet';
const ANCHOR_NAME = 'k6PerfAnchor';

export const options = {
    setupTimeout: '30m',
    teardownTimeout: '20m',
    scenarios: testConfig.scenarios,
    thresholds: testConfig.thresholds,
};

export function setup() {
    const startTimeInMillis = Date.now();

    let numberOfSuccessfulRegistrations = 0;
    const registrations = [
        [createDataspace(DATASPACE_NAME), 'create dataspace status equals 201'],
        [createSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME), 'create schema set status equals 201'],
        [createAnchor(DATASPACE_NAME, SCHEMA_SET_NAME, ANCHOR_NAME), 'create anchor status equals 201'],
    ];
    for (const [response, checkLabel] of registrations) {
        if (check(response, { [checkLabel]: (response) => response.status === 201 })) {
            numberOfSuccessfulRegistrations++;
        }
    }

    const totalRegistrationTimeInSeconds = (Date.now() - startTimeInMillis) / 1000.0;
    kpiTrendDeclarations.admin_crud_registrations.add(numberOfSuccessfulRegistrations / totalRegistrationTimeInSeconds);
}

export function teardown() {
    check(deleteAnchor(DATASPACE_NAME, ANCHOR_NAME), { 'delete anchor status equals 204': (response) => response.status === 204 });
    check(deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME), { 'delete schema set status equals 204': (response) => response.status === 204 });
    check(deleteDataspace(DATASPACE_NAME), { 'delete dataspace status equals 204': (response) => response.status === 204 });

    sleep(CONTAINER_COOL_DOWN_TIME_IN_SECONDS);
}

// No-op default: keeps k6 happy while `scenarios` is empty (no scenarios wired up yet).
// Remove once a scenario is added to config/kpi.json + config/endurance.json.
export default function () {
}

export function handleSummary(data) {
    const testProfile = __ENV.TEST_PROFILE || 'kpi';
    if (testProfile === 'kpi') {
        console.log("✅ Generating KPI summary...");
        return {
            stdout: makeCustomSummaryReport(data, options),
        };
    }
    console.log("⛔ Skipping KPI summary (not in 'kpi' profile)");
    return {};
}
