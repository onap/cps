/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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
import http from 'k6/http';
import {
    performPostRequest, getAlternateId, NCMP_BASE_URL, DMI_PLUGIN_URL, TOTAL_CM_HANDLES, MODULE_SET_TAGS, ACTUATOR_PROMETHEUS_URL
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

/**
 * Polls the cm-handle-state instrumentation until all registered CM handles are READY.
 *
 * The measured value is the instrumentation counter
 * `cps_ncmp_inventory_cm_handles_by_state{state="READY"}` (exposed on the actuator
 * prometheus endpoint), which reflects the LCM events actually sent - this is the
 * source of truth for CPS-3339 and can differ from the cps-path query count.
 *
 * There is deliberately NO timeout on the shortfall case: if some handles never reach
 * READY the loop keeps polling and the overall job execution timeout ends the run.
 * Failing the whole (hourly) job is the intended, stronger signal that something is wrong.
 *
 * The loop does break if more handles are READY than were registered (readyCount >
 * TOTAL_CM_HANDLES), as that is a distinct, explainable fault (e.g. stale data from a
 * previous run) and looping forever would not help.
 *
 * @returns {number} The final instrumented ready CM-handle count, for the caller to assert on.
 */
/**
 * Polls the CM-handle state until every registered CM handle has actually reached READY in the DB, and none is
 * left stuck in ADVISED or LOCKED.
 *
 * The source of truth here is the cps-path query (actual persisted CM-handle state), NOT the
 * `cps_ncmp_inventory_cm_handles_by_state{state="READY"}` instrumentation gauge. The gauge counts READY LCM events
 * sent, which on a multi-instance deployment can reach TOTAL_CM_HANDLES even when an individual handle is stuck
 * ADVISED (the instance that won an anchor-creation race still emits the event). Only a per-state DB query can
 * detect a handle that never transitioned - which is exactly the CPS-3344 multi-instance regression.
 *
 * There is deliberately NO timeout on the shortfall case: if some handles never reach READY the loop keeps polling
 * and the overall job execution timeout ends the run. Failing the whole (hourly) job is the intended, stronger
 * signal that something is wrong.
 *
 * On every run (whether all handles converged or not) a final summary is logged with all per-state DB counts and
 * the instrumentation gauge, so the full picture is always visible; a discrepancy is additionally logged as an error.
 *
 * @returns {{ready: number, advised: number, locked: number}} the final per-state counts, for the caller to assert on.
 */
export function waitForAllCmHandlesToBeReady() {
    const POLLING_INTERVAL_SECONDS = 5;
    let readyCount = 0;
    let advisedCount = 0;
    let lockedCount = 0;
    do {
        sleep(POLLING_INTERVAL_SECONDS);
        readyCount = getCmHandleCountByState('cps-path-for-ready-cm-handles');
        advisedCount = getCmHandleCountByState('cps-path-for-advised-cm-handles');
        lockedCount = getCmHandleCountByState('cps-path-for-locked-cm-handles');
        console.log(`${readyCount}/${TOTAL_CM_HANDLES} CM handles are READY (advised: ${advisedCount}, `
            + `locked: ${lockedCount}) per cps-path query`);
        if (readyCount > TOTAL_CM_HANDLES) {
            console.error(`❌ More CM handles are READY (${readyCount}) than were registered (${TOTAL_CM_HANDLES}). `
                + `Check logs for duplication between instances.`);
            break;
        }
    } while (readyCount < TOTAL_CM_HANDLES || advisedCount > 0 || lockedCount > 0);

    // Always log the final counts (DB per-state plus the instrumentation gauge) whether the outcome is correct or
    // not, so every run shows the full picture. A discrepancy is additionally flagged as an error.
    const readyGaugeFromInstrumentation = getReadyCmHandlesCountFromInstrumentation();
    const eventAndDbCountMatch = readyCount === TOTAL_CM_HANDLES && advisedCount === 0 && lockedCount === 0;
    const summary = `Final CM handle state. Registered: ${TOTAL_CM_HANDLES}, `
        + `READY (cps-path): ${readyCount}, ADVISED: ${advisedCount}, LOCKED: ${lockedCount}, `
        + `READY LCM events sent (instrumentation): ${readyGaugeFromInstrumentation}.`;
    if (eventAndDbCountMatch) {
        console.log(`✅ ${summary} All CM handles reached READY.`);
    } else {
        console.error(`❌ ${summary} Handles stuck in ADVISED/LOCKED. Multi-instance module-sync race? (CPS-3344).`);
    }

    return { ready: readyCount, advised: advisedCount, locked: lockedCount };
}

function createCmHandlePayload(cmHandleIds) {
    return {
        "dmiPlugin": DMI_PLUGIN_URL,
        "createdCmHandles": cmHandleIds.map((cmHandleId, index) => {
            // Ensure unique networkSegment within range 1-10
            let networkSegmentId = Math.floor(Math.random() * 10) + 1;
            let moduleTag = MODULE_SET_TAGS[index % MODULE_SET_TAGS.length];

            return {
                "cmHandle": cmHandleId,
                "alternateId": getAlternateId(cmHandleId.replace('ch-', '')),
                "moduleSetTag": moduleTag,
                "dataProducerIdentifier": "some-data-producer-id",
                "cmHandleProperties": {
                    "segmentId": index + 1,
                    "networkSegment": `Region=NorthAmerica,Segment=${networkSegmentId}`,
                    "deviceIdentifier": `Element=RadioBaseStation_5G_${index + 1000}`,
                    "hardwareVersion": `HW-${moduleTag}`,
                    "softwareVersion": `Firmware_${moduleTag}`,
                    "syncStatus": "ACTIVE",
                    "nodeCategory": "VirtualNode"
                },
                "publicCmHandleProperties": {
                    "systemId": index + 1,
                    "systemName": "ncmp"
                }
            };
        }),
    };
}

/**
 * Reads the READY cm-handle count from the instrumentation gauge
 * `cps_ncmp_inventory_cm_handles_by_state{state="READY"}` on the actuator
 * prometheus endpoint. This gauge is driven by LCM state changes, so it is the
 * source of truth for how many handles NCMP considers READY.
 *
 * @returns {number} The instrumented READY count, or 0 if it cannot be read/parsed.
 */
function getReadyCmHandlesCountFromInstrumentation() {
    const url = ACTUATOR_PROMETHEUS_URL;
    const response = http.get(url, { tags: { endpoint: 'actuator-prometheus' } });
    if (response.status !== 200) {
        console.error(`❌ Failed to read instrumentation metrics (status ${response.status}) from ${url}`);
        return 0;
    }
    // Matches a line like: cps_ncmp_inventory_cm_handles_by_state{...,state="READY",...} 4980.0
    const readyGaugeRegex = /^cps_ncmp_inventory_cm_handles_by_state\{[^}]*state="READY"[^}]*\}\s+([0-9.eE+-]+)/m;
    const match = readyGaugeRegex.exec(response.body);
    if (match === null) {
        console.error(`❌ Could not find READY cm-handle-state gauge in metrics from ${url}`);
        return 0;
    }
    return Math.round(parseFloat(match[1]));
}

function getCmHandleCountByState(scenario) {
    const response = executeCmHandleIdSearch(scenario);
    const arrayOfCmHandleIds = JSON.parse(response.body);
    return arrayOfCmHandleIds.length;
}
