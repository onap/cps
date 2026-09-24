/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

/**
 * Functional integration test for the DMI -> NCMP -> SDNC cm-handle onboarding
 * flow, exercised through DMI's own API.
 *
 * Replaces the CSIT Robot suite that used to live under
 * csit/tests/dmi-integration/ (dmi-ncmp.robot, dmi-sdnc.robot).
 *
 * Covers, in order:
 *   1. DMI plugin liveness and readiness
 *   2. CPS-NCMP liveness and readiness
 *   3. Register one cm-handle (the pnfsim netconf simulator)
 *   4. Wait for the cm-handle to reach READY, with a timeout
 *   5. Retrieve the modules and assert the expected module count
 *
 * Target environment: the CM stack deployed by cps-charts
 * (--set onapDmiStack.enabled=true), i.e. DMI + cps-ncmp + SDNC + pnfsim with the
 * simulator mounted as a cm handle.
 *
 * Run with:
 *   k6 run onap-dmi-stack-test-runner.js -e TEST_PROFILE=functional
 */

import {check} from 'k6';
import {
    testConfig,
    validateResponseStatus,
    CM_HANDLE,
    EXPECTED_NUMBER_OF_MODULES
} from './common/utils.js';
import {
    getDmiLiveness,
    getDmiReadiness,
    getNcmpLiveness,
    getNcmpReadiness,
    isHealthy
} from './common/health-checks.js';
import {
    registerCmHandle,
    deregisterCmHandleIfPresent,
    getModuleReferences,
    countDistinctModules,
    waitForCmHandleToBeReady
} from './common/dmi-crud.js';

export const options = {
    setupTimeout: '2m',
    teardownTimeout: '2m',
    scenarios: testConfig.scenarios,
    thresholds: testConfig.thresholds,
};

/**
 * Clears a cm handle left behind by a previous run, so registration is not
 * rejected with 'cm-handle already exists'. Makes the test repeatable.
 */
export function setup() {
    deregisterCmHandleIfPresent();
}

export function onapDmiStackScenario() {
    checkDmiPluginHealth();
    checkNcmpHealth();
    registerCmHandleWithDmi();
    waitUntilCmHandleIsReady();
    retrieveModules();
}

/**
 * A/C: healthcheck (liveness/readiness of dmi plugin).
 */
function checkDmiPluginHealth() {
    const livenessResponse = getDmiLiveness();
    check(livenessResponse, {
        'DMI plugin liveness is UP': () => isHealthy(livenessResponse),
    });

    const readinessResponse = getDmiReadiness();
    check(readinessResponse, {
        'DMI plugin readiness is UP': () => isHealthy(readinessResponse),
    });
}

/**
 * A/C: healthcheck (liveness/readiness of cps ncmp).
 */
function checkNcmpHealth() {
    const livenessResponse = getNcmpLiveness();
    check(livenessResponse, {
        'CPS-NCMP liveness is UP': () => isHealthy(livenessResponse),
    });

    const readinessResponse = getNcmpReadiness();
    check(readinessResponse, {
        'CPS-NCMP readiness is UP': () => isHealthy(readinessResponse),
    });
}

/**
 * A/C: register 1 cm-handle (pnfsimulator).
 *
 * The DMI OpenAPI spec defines 201 Created for this endpoint, and the deployed
 * plugin returns 201, so that is what is asserted.
 */
function registerCmHandleWithDmi() {
    const httpResponse = registerCmHandle();
    check(httpResponse, {
        'register cm handle status is 201': (response) =>
            validateResponseStatus(response, 201, 'register cm handle'),
    });
}

/**
 * A/C: wait until status is ready and timeout (if its not ready).
 */
function waitUntilCmHandleIsReady() {
    const cmHandleState = waitForCmHandleToBeReady();
    check(cmHandleState, {
        'CM handle reaches READY': (state) => state === 'READY',
    });
}

/**
 * A/C: retrieve the modules and check the number of modules.
 *
 * DMI returns each module twice, so the distinct module names are counted rather
 * than the raw array length.
 */
function retrieveModules() {
    const httpResponse = getModuleReferences();
    check(httpResponse, {
        'get all modules status is 200': (response) =>
            validateResponseStatus(response, 200, 'get all modules'),
    });

    const numberOfModules = countDistinctModules(httpResponse);
    const isExpectedCount = check(numberOfModules, {
        [`number of modules is ${EXPECTED_NUMBER_OF_MODULES}`]: (count) =>
            count === EXPECTED_NUMBER_OF_MODULES,
    });

    if (isExpectedCount) {
        console.log(`✅ Retrieved ${numberOfModules} modules for ${CM_HANDLE}`);
    } else {
        console.error(`❌ Expected ${EXPECTED_NUMBER_OF_MODULES} modules for ${CM_HANDLE} `
            + `but found ${numberOfModules}`);
    }
}

/**
 * Removes the cm handle so the stack is left as it was found.
 */
export function teardown() {
    deregisterCmHandleIfPresent();
}
