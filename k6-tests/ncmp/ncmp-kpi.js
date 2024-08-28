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
    makeCustomSummaryReport, makeBatchOfCmHandleIds, DATA_OPERATION_READ_BATCH_SIZE,
    TOPIC_DATA_OPERATIONS_BATCH_READ, KAFKA_BOOTSTRAP_SERVERS, REGISTRATION_BATCH_SIZE
} from './common/utils.js';
import {
    createCmHandles,
    deleteCmHandles,
    waitForAllCmHandlesToBeReady
} from './common/cmhandle-crud.js';
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead, passthroughReadWithAltId, passthroughWrite, batchRead } from './common/passthrough-crud.js';

let cmHandlesCreatedPerSecondTrend = new Trend('cmhandles_created_per_second', false);
let cmHandlesDeletedPerSecondTrend = new Trend('cmhandles_deleted_per_second', false);
let passthroughReadNcmpOverheadTrend = new Trend('ncmp_overhead_passthrough_read', true);
let passthroughReadNcmpOverheadTrendWithAlternateId = new Trend('ncmp_overhead_passthrough_read_alt_id', true);
let passthroughWriteNcmpOverheadTrend = new Trend('ncmp_overhead_passthrough_write', true);
let idSearchDurationTrend = new Trend('id_search_duration', true);
let cmSearchDurationTrend = new Trend('cm_search_duration', true);
let dataOperationsBatchReadCmHandlePerSecondTrend = new Trend('data_operations_batch_read_cmhandles_per_second');

const reader = new Reader({
    brokers: KAFKA_BOOTSTRAP_SERVERS,
    topic: TOPIC_DATA_OPERATIONS_BATCH_READ,
});

const DURATION = '15m';

export const options = {
    setupTimeout: '6m',
    teardownTimeout: '6m',
    scenarios: {
        passthrough_read: {
            executor: 'constant-vus',
            exec: 'passthrough_read',
            vus: 9,
            duration: DURATION,
        },
        passthrough_read_alt_id: {
            executor: 'constant-vus',
            exec: 'passthrough_read_alt_id',
            vus: 1,
            duration: DURATION,
        },
        passthrough_write: {
            executor: 'constant-vus',
            exec: 'passthrough_write',
            vus: 10,
            duration: DURATION,
        },
        id_search_module: {
            executor: 'constant-vus',
            exec: 'id_search_module',
            vus: 3,
            duration: DURATION,
        },
        cm_search_module: {
            executor: 'constant-vus',
            exec: 'cm_search_module',
            vus: 3,
            duration: DURATION,
        },
        data_operation_send_async_http_request: {
            executor: 'constant-arrival-rate',
            exec: 'data_operation_send_async_http_request',
            duration: DURATION,
            rate: 1,
            timeUnit: '1s',
            preAllocatedVUs: 1,
        },
        data_operation_async_batch_read: {
            executor: 'per-vu-iterations',
            exec: 'data_operation_async_batch_read',
            vus: 1,
            iterations: 1,
            maxDuration: DURATION,
        }
    },
    thresholds: {
        'cmhandles_created_per_second': ['avg >= 22'],
        'cmhandles_deleted_per_second': ['avg >= 22'],
        'ncmp_overhead_passthrough_read': ['avg <= 100'],
        'ncmp_overhead_passthrough_read_alt_id': ['avg <= 100'],
        'ncmp_overhead_passthrough_write': ['avg <= 100'],
        'id_search_duration': ['avg <= 625'],
        'cm_search_duration': ['avg <= 13000'],
        'http_req_failed{scenario:id_search_module}': ['rate == 0'],
        'http_req_failed{scenario:cm_search_module}': ['rate == 0'],
        'http_req_failed{scenario:passthrough_read}': ['rate == 0'],
        'http_req_failed{scenario:passthrough_write}': ['rate == 0'],
        'http_req_failed{group:::setup}':['rate == 0'],
        'http_req_failed{group:::teardown}':['rate == 0'],
        'http_req_failed{scenario:data_operation_send_async_http_request}': ['rate == 0'],
        'kafka_reader_error_count{scenario:data_operation_consume_kafka_responses}': ['count == 0'],
        'data_operations_batch_read_cmhandles_per_second': ['avg >= 150'],
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

    const TOTAL_BATCHES = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < TOTAL_BATCHES; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = deleteCmHandles(nextBatchOfCmHandleIds);
        check(response, { 'delete CM-handles status equals 200': (r) => r.status === 200 });
    }

    const endTimeInMillis = Date.now();
    const totalDeregistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    cmHandlesDeletedPerSecondTrend.add(TOTAL_CM_HANDLES / totalDeregistrationTimeInSeconds);
}

export function passthrough_read() {
    const response = passthroughRead();
    if (check(response, { 'passthrough read status equals 200': (r) => r.status === 200 })) {
        const overhead = response.timings.duration - READ_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughReadNcmpOverheadTrend.add(overhead);
    }
}

export function passthrough_read_alt_id() {
    const response = passthroughReadWithAltId();
    if (check(response, { 'passthrough read with alternate Id status equals 200': (r) => r.status === 200 })) {
        const overhead = response.timings.duration - READ_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughReadNcmpOverheadTrendWithAlternateId.add(overhead);
    }
}

export function passthrough_write() {
    const response = passthroughWrite();
    if (check(response, { 'passthrough write status equals 201': (r) => r.status === 201 })) {
        const overhead = response.timings.duration - WRITE_DATA_FOR_CM_HANDLE_DELAY_MS;
        passthroughWriteNcmpOverheadTrend.add(overhead);
    }
}

export function id_search_module() {
    const response = executeCmHandleIdSearch('module');
    if (check(response, { 'CM handle ID search status equals 200': (r) => r.status === 200 })) {
        check(response, { 'CM handle ID search returned expected CM-handles': (r) => r.json('#') === TOTAL_CM_HANDLES });
        idSearchDurationTrend.add(response.timings.duration);
    }
}

export function cm_search_module() {
    const response = executeCmHandleSearch('module');
    if (check(response, { 'CM handle search status equals 200': (r) => r.status === 200 })) {
        check(response, { 'CM handle search returned expected CM-handles': (r) => r.json('#') === TOTAL_CM_HANDLES });
        cmSearchDurationTrend.add(response.timings.duration);
    }
}

export function data_operation_send_async_http_request() {
    const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(DATA_OPERATION_READ_BATCH_SIZE, 0);
    const response = batchRead(nextBatchOfCmHandleIds);
    check(response, { 'data operation batch read status equals 200': (r) => r.status === 200 });
}

export function data_operation_async_batch_read() {
    /*
        The DATA_OPERATION_READ_BATCH_SIZE is 200.
        The batch read requests is being sent every second for 15 minutes.
        Thus, the expected total number of messages to be consumed 180000.
     */
    const TOTAL_MESSAGES_TO_CONSUME = 180000;
    try {
        let messagesConsumed = 0;
        let startTime = Date.now();

        while (messagesConsumed < TOTAL_MESSAGES_TO_CONSUME) {
            let messages = reader.consume({ limit: TOTAL_MESSAGES_TO_CONSUME - messagesConsumed });

            if (messages.length > 0) {
                messagesConsumed += messages.length;
            }
        }

        let endTime = Date.now();
        const totalRegistrationTimeInSeconds = (endTime - startTime) / 1000.0;
        dataOperationsBatchReadCmHandlePerSecondTrend.add(TOTAL_MESSAGES_TO_CONSUME / totalRegistrationTimeInSeconds);
    } catch (error) {
        dataOperationsBatchReadCmHandlePerSecondTrend.add(0);
        console.error(error);
    }
}

export function handleSummary(data) {
    return {
        stdout: makeCustomSummaryReport(data, options),
    };
}
