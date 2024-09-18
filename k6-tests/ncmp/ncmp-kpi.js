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

import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { Reader } from 'k6/x/kafka';
import {
    TOTAL_CM_HANDLES, READ_DATA_FOR_CM_HANDLE_DELAY_MS, WRITE_DATA_FOR_CM_HANDLE_DELAY_MS,
    makeCustomSummaryReport, makeBatchOfCmHandleIds, LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE,
    LEGACY_BATCH_TOPIC_NAME, KAFKA_BOOTSTRAP_SERVERS, REGISTRATION_BATCH_SIZE,
    LEGACY_BATCH_THROUGHPUT_TEST_NUMBER_OF_REQUESTS
} from './common/utils.js';
import { createCmHandles, deleteCmHandles, waitForAllCmHandlesToBeReady } from './common/cmhandle-crud.js';
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead, passthroughWrite, legacyBatchRead } from './common/passthrough-crud.js';

let cmHandlesCreatedPerSecondTrend = new Trend('cmhandles_created_per_second', false);
let cmHandlesDeletedPerSecondTrend = new Trend('cmhandles_deleted_per_second', false);
let passthroughReadNcmpOverheadTrend = new Trend('ncmp_overhead_passthrough_read', true);
let passthroughReadNcmpOverheadTrendWithAlternateId = new Trend('ncmp_overhead_passthrough_read_alt_id', true);
let passthroughWriteNcmpOverheadTrend = new Trend('ncmp_overhead_passthrough_write', true);
let passthroughWriteNcmpOverheadTrendWithAlternateId = new Trend('ncmp_overhead_passthrough_write_alt_id', true);
let idSearchDurationTrend = new Trend('id_search_duration', true);
let cmSearchDurationTrend = new Trend('cm_search_duration', true);
let legacyBatchReadCmHandlesPerSecondTrend = new Trend('legacy_batch_read_cmhandles_per_second', false);

const legacyBatchEventReader = new Reader({
    brokers: KAFKA_BOOTSTRAP_SERVERS,
    topic: LEGACY_BATCH_TOPIC_NAME,
});

const DURATION = '15m';
const LEGACY_BATCH_THROUGHPUT_TEST_START_TIME = '930s';

export const options = {
    setupTimeout: '8m',
    teardownTimeout: '6m',
    scenarios: {
        passthrough_read_scenario: {
            executor: 'constant-vus',
            exec: 'passthroughReadScenario',
            vus: 4,
            duration: DURATION,
        },
        passthrough_read_alt_id_scenario: {
            executor: 'constant-vus',
            exec: 'passthroughReadAltIdScenario',
            vus: 4,
            duration: DURATION,
        },
        passthrough_write_scenario: {
            executor: 'constant-vus',
            exec: 'passthroughWriteScenario',
            vus: 4,
            duration: DURATION,
        },
        passthrough_write_alt_id_scenario: {
            executor: 'constant-vus',
            exec: 'passthroughWriteAltIdScenario',
            vus: 4,
            duration: DURATION,
        },
        cm_handle_id_search_scenario: {
            executor: 'constant-vus',
            exec: 'cmHandleIdSearchScenario',
            vus: 5,
            duration: DURATION,
        },
        cm_handle_search_scenario: {
            executor: 'constant-vus',
            exec: 'cmHandleSearchScenario',
            vus: 5,
            duration: DURATION,
        },
        legacy_batch_produce_scenario: {
            executor: 'shared-iterations',
            exec: 'legacyBatchProduceScenario',
            vus: 2,
            iterations: LEGACY_BATCH_THROUGHPUT_TEST_NUMBER_OF_REQUESTS,
            startTime: LEGACY_BATCH_THROUGHPUT_TEST_START_TIME,
        },
        legacy_batch_consume_scenario: {
            executor: 'per-vu-iterations',
            exec: 'legacyBatchConsumeScenario',
            vus: 1,
            iterations: 1,
            startTime: LEGACY_BATCH_THROUGHPUT_TEST_START_TIME,
        }
    },
    thresholds: {
        'http_req_failed': ['rate == 0'],
        'cmhandles_created_per_second': ['avg >= 22'],
        'cmhandles_deleted_per_second': ['avg >= 22'],
        'ncmp_overhead_passthrough_read': ['avg <= 40'],
        'ncmp_overhead_passthrough_write': ['avg <= 40'],
        'ncmp_overhead_passthrough_read_alt_id': ['avg <= 40'],
        'ncmp_overhead_passthrough_write_alt_id': ['avg <= 40'],
        'id_search_duration': ['avg <= 2000'],
        'cm_search_duration': ['avg <= 15000'],
        'legacy_batch_read_cmhandles_per_second': ['avg >= 150'],
    },
};

export function setup() {
    const startTimeInMillis = Date.now();

    const TOTAL_BATCHES = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < TOTAL_BATCHES; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = createCmHandles(nextBatchOfCmHandleIds);
        check(response, { 'create CM-handles status equals 200': (r) => r.status === 200 });
    }

    waitForAllCmHandlesToBeReady();

    const endTimeInMillis = Date.now();
    const totalRegistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    cmHandlesCreatedPerSecondTrend.add(TOTAL_CM_HANDLES / totalRegistrationTimeInSeconds);
}

export function teardown() {
    const startTimeInMillis = Date.now();

    let DEREGISTERED_CM_HANDLES = 0
    const TOTAL_BATCHES = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < TOTAL_BATCHES; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = deleteCmHandles(nextBatchOfCmHandleIds);
        if (response.error_code === 0) {
              DEREGISTERED_CM_HANDLES += REGISTRATION_BATCH_SIZE
        }
        check(response, { 'delete CM-handles status equals 200': (r) => r.status === 200 });
    }

    const endTimeInMillis = Date.now();
    const totalDeregistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    cmHandlesDeletedPerSecondTrend.add(DEREGISTERED_CM_HANDLES / totalDeregistrationTimeInSeconds);
}

export function passthroughReadScenario() {
    const response = passthroughRead(false);
    if (check(response, { 'passthrough read status equals 200': (r) => r.status === 200 })) {
        const overhead = response.timings.duration - READ_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughReadNcmpOverheadTrend.add(overhead);
    }
}

export function passthroughReadAltIdScenario() {
    const response = passthroughRead(true);
    if (check(response, { 'passthrough read with alternate Id status equals 200': (r) => r.status === 200 })) {
        const overhead = response.timings.duration - READ_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughReadNcmpOverheadTrendWithAlternateId.add(overhead);
    }
}

export function passthroughWriteScenario() {
    const response = passthroughWrite(false);
    if (check(response, { 'passthrough write status equals 201': (r) => r.status === 201 })) {
        const overhead = response.timings.duration - WRITE_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughWriteNcmpOverheadTrend.add(overhead);
    }
}

export function passthroughWriteAltIdScenario() {
    const response = passthroughWrite(true);
    if (check(response, { 'passthrough write with alternate Id status equals 201': (r) => r.status === 201 })) {
        const overhead = response.timings.duration - WRITE_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughWriteNcmpOverheadTrendWithAlternateId.add(overhead);
    }
}

export function cmHandleIdSearchScenario() {
    const response = executeCmHandleIdSearch('module-and-properties');
    if (check(response, { 'CM handle ID search status equals 200': (r) => r.status === 200 })
     && check(response, { 'CM handle ID search returned expected CM-handles': (r) => r.json('#') === TOTAL_CM_HANDLES })) {
        idSearchDurationTrend.add(response.timings.duration);
    }
}

export function cmHandleSearchScenario() {
    const response = executeCmHandleSearch('module-and-properties');
    if (check(response, { 'CM handle search status equals 200': (r) => r.status === 200 })
     && check(response, { 'CM handle search returned expected CM-handles': (r) => r.json('#') === TOTAL_CM_HANDLES })) {
        cmSearchDurationTrend.add(response.timings.duration);
    }
}

export function legacyBatchProduceScenario() {
    const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE, 0);
    const response = legacyBatchRead(nextBatchOfCmHandleIds);
    check(response, { 'data operation batch read status equals 200': (r) => r.status === 200 });
}

export function legacyBatchConsumeScenario() {
    const TOTAL_MESSAGES_TO_CONSUME = LEGACY_BATCH_THROUGHPUT_TEST_NUMBER_OF_REQUESTS * LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE;
    try {
        let messagesConsumed = 0;
        let startTime = Date.now();

        while (messagesConsumed < TOTAL_MESSAGES_TO_CONSUME) {
            let messages = legacyBatchEventReader.consume({ limit: 1000 });

            if (messages.length > 0) {
                messagesConsumed += messages.length;
            }
        }

        let endTime = Date.now();
        const timeToConsumeMessagesInSeconds = (endTime - startTime) / 1000.0;
        legacyBatchReadCmHandlesPerSecondTrend.add(TOTAL_MESSAGES_TO_CONSUME / timeToConsumeMessagesInSeconds);
    } catch (error) {
        legacyBatchReadCmHandlesPerSecondTrend.add(0);
        console.error(error);
    }
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
