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

import {sleep} from 'k6';
import http from 'k6/http';
import {
    performGetRequest,
    performPostRequest,
    CONTENT_TYPE_JSON_PARAM,
    DMI_BASE_URL,
    NCMP_BASE_URL,
    DMI_PLUGIN_IN_CLUSTER_URL,
    CM_HANDLE,
    CM_HANDLE_READY_TIMEOUT_IN_SECONDS,
    CM_HANDLE_READY_POLL_INTERVAL_IN_SECONDS
} from './utils.js';

/**
 * Registers the cm handle with DMI's inventory endpoint, which forwards the
 * registration on to NCMP.
 *
 * @returns {Object} The HTTP response.
 */
export function registerCmHandle() {
    const url = `${DMI_BASE_URL}/dmi/v1/inventory/cmHandles`;
    const payload = JSON.stringify({cmHandles: [CM_HANDLE]});
    return performPostRequest(url, payload, 'registerCmHandles');
}

/**
 * Fetches all YANG modules for the cm handle via DMI, which reads them from the
 * netconf simulator through SDNC.
 *
 * @returns {Object} The HTTP response.
 */
export function getModuleReferences() {
    const url = `${DMI_BASE_URL}/dmi/v1/ch/${CM_HANDLE}/modules`;
    return performPostRequest(url, '{}', 'getModuleReferences');
}

/**
 * Counts the distinct YANG modules in a DMI ModuleSet response.
 *
 * DMI returns each module twice (44 entries for pnfsim's 22 modules), so this
 * de-duplicates by module name. Revisions are identical across the pair, so the
 * name alone identifies a module here.
 *
 * @param {Object} httpResponse - The response from getModuleReferences().
 * @returns {number} The number of distinct module names.
 */
export function countDistinctModules(httpResponse) {
    return getDistinctModuleNames(httpResponse).length;
}

/**
 * Extracts the distinct module names from a DMI ModuleSet response.
 *
 * @param {Object} httpResponse - The response from getModuleReferences().
 * @returns {string[]} The distinct module names.
 */
export function getDistinctModuleNames(httpResponse) {
    if (httpResponse.status !== 200) {
        return [];
    }
    try {
        const moduleSet = httpResponse.json();
        if (!moduleSet || !Array.isArray(moduleSet.schemas)) {
            return [];
        }
        const distinctModuleNames = new Set();
        for (const schema of moduleSet.schemas) {
            distinctModuleNames.add(schema.moduleName);
        }
        return Array.from(distinctModuleNames);
    } catch (error) {
        console.error(`❌ Could not parse module references response: ${error.message}`);
        return [];
    }
}

/**
 * Polls NCMP until the cm handle reaches READY, so module retrieval is not
 * attempted before module sync has completed.
 *
 * Returns on a bounded timeout rather than looping indefinitely: a single
 * cm-handle that never syncs is a functional failure worth reporting promptly.
 *
 * @returns {string} The last observed state, for the caller to assert on.
 */
export function waitForCmHandleToBeReady() {
    const deadlineInMillis = Date.now() + CM_HANDLE_READY_TIMEOUT_IN_SECONDS * 1000;
    let lastObservedState = 'UNKNOWN';
    while (Date.now() < deadlineInMillis) {
        sleep(CM_HANDLE_READY_POLL_INTERVAL_IN_SECONDS);
        lastObservedState = readCmHandleState();
        console.log(`CM handle ${CM_HANDLE} state is ${lastObservedState}`);
        if (lastObservedState === 'READY') {
            return lastObservedState;
        }
        if (lastObservedState === 'LOCKED') {
            console.error(`❌ CM handle ${CM_HANDLE} is LOCKED, module sync will not complete. `
                + `Check the ncmp-dmi-plugin and SDNC logs.`);
            return lastObservedState;
        }
    }
    console.error(`❌ CM handle ${CM_HANDLE} did not reach READY within `
        + `${CM_HANDLE_READY_TIMEOUT_IN_SECONDS}s, last state was ${lastObservedState}.`);
    return lastObservedState;
}

/**
 * Reads and parses the current cm-handle state from NCMP.
 *
 * @returns {string} The cm-handle state, or 'UNKNOWN' if it cannot be read.
 */
function readCmHandleState() {
    const httpResponse = performGetRequest(
        `${NCMP_BASE_URL}/ncmp/v1/ch/${CM_HANDLE}/state`, 'getCmHandleState');
    if (httpResponse.status !== 200) {
        console.error(`❌ Failed to read CM handle state (status ${httpResponse.status}).`);
        return 'UNKNOWN';
    }
    try {
        const cmHandleState = httpResponse.json('state.cmHandleState');
        return cmHandleState ? cmHandleState : 'UNKNOWN';
    } catch (error) {
        console.error(`❌ Could not parse CM handle state response: ${error.message}`);
        return 'UNKNOWN';
    }
}

/**
 * Removes the cm handle if it is already registered, so the test is repeatable.
 *
 * DMI's inventory API only registers cm handles; it has no de-register operation,
 * so this goes directly to NCMP. Without it a leftover cm handle makes NCMP
 * answer with errorCode 109 ('cm-handle already exists'), which DMI surfaces as a
 * 500.
 *
 * A non-2xx outcome is expected when there is nothing to remove, so this is
 * excluded from http_req_failed.
 *
 * @returns {Object} The HTTP response.
 */
export function deregisterCmHandleIfPresent() {
    const payload = JSON.stringify({
        "dmiPlugin": DMI_PLUGIN_IN_CLUSTER_URL,
        "removedCmHandles": [CM_HANDLE]
    });
    return http.post(`${NCMP_BASE_URL}/ncmpInventory/v1/ch`, payload, {
        headers: CONTENT_TYPE_JSON_PARAM,
        tags: {endpoint: 'deregisterCmHandleIfPresent'},
        responseCallback: http.expectedStatuses({min: 200, max: 599}),
    });
}
