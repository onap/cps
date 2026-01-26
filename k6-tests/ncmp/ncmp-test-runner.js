/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
import { Reader } from 'k6/x/kafka';
import {
    testConfig,
    validateResponseAndRecordMetricWithOverhead,
    validateResponseAndRecordMetric,
    makeCustomSummaryReport,
    makeBatchOfCmHandleIds,
    makeRandomBatchOfAlternateIds,
    TOTAL_CM_HANDLES,
    READ_DATA_FOR_CM_HANDLE_DELAY_MS,
    WRITE_DATA_FOR_CM_HANDLE_DELAY_MS,
    LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE,
    REGISTRATION_BATCH_SIZE,
    KAFKA_BOOTSTRAP_SERVERS,
    LEGACY_BATCH_TOPIC_NAME,
    CONTAINER_COOL_DOWW_TIME_IN_SECONDS
} from './common/utils.js';
import { createCmHandles, deleteCmHandles, waitForAllCmHandlesToBeReady } from './common/cmhandle-crud.js';
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead, passthroughWrite, legacyBatchRead } from './common/passthrough-crud.js';
import { sendBatchOfKafkaMessages } from './common/produce-avc-event.js';
import { executeWriteDataJob } from "./common/write-data-job.js";
import { provMnSReadOperation, provMnSWriteOperation } from "./common/provmns-crud.js";


const throughputTrends = ['cm_handles_created', 'cm_handles_deleted', 'legacy_batch_read'];
const kpiTrendDeclarations = {};

for (const trendName of open('./config/trendNames.txt').trim().split('\n')) {
    const isTimeTrend = !throughputTrends.includes(trendName);
    kpiTrendDeclarations[trendName] = new Trend(trendName, isTimeTrend);
}

const EXPECTED_WRITE_RESPONSE_COUNT = 1;

export const legacyBatchEventReader = new Reader({
    brokers: [KAFKA_BOOTSTRAP_SERVERS],
    topic: LEGACY_BATCH_TOPIC_NAME,
    maxWait: '500ms', // Do not increase otherwise the read won't finish within 1 second (it is set to run every second)
});

export const options = {
    setupTimeout: '30m',
    teardownTimeout: '20m',
    scenarios: testConfig.scenarios,
    thresholds: testConfig.thresholds,
};

export function setup() {
    const startTimeInMillis = Date.now();

    const numberOfBatches = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < numberOfBatches; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = createCmHandles(nextBatchOfCmHandleIds);
        check(response, { 'create CM-handles status equals 200': (response) => response.status === 200 });
    }

    waitForAllCmHandlesToBeReady();

    const endTimeInMillis = Date.now();
    const totalRegistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    kpiTrendDeclarations.cm_handles_created.add(TOTAL_CM_HANDLES / totalRegistrationTimeInSeconds);
}

export function teardown() {
    const startTimeInMillis = Date.now();

    let numberOfDeregisteredCmHandles = 0
    const numberOfBatches = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < numberOfBatches; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = deleteCmHandles(nextBatchOfCmHandleIds);
        if (response.error_code === 0) {
            numberOfDeregisteredCmHandles += REGISTRATION_BATCH_SIZE
        }
        check(response, { 'delete CM-handles status equals 200': (response) => response.status === 200 });
    }

    const endTimeInMillis = Date.now();
    const totalDeregistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    kpiTrendDeclarations.cm_handles_deleted.add(numberOfDeregisteredCmHandles / totalDeregistrationTimeInSeconds);
    sleep(CONTAINER_COOL_DOWW_TIME_IN_SECONDS);
}

export function passthroughReadAltIdScenario() {
    const response = passthroughRead();
    validateResponseAndRecordMetricWithOverhead(response, 200, 'passthrough read with alternate Id status equals 200', READ_DATA_FOR_CM_HANDLE_DELAY_MS, kpiTrendDeclarations.ncmp_read_overhead);
}

export function passthroughWriteAltIdScenario() {
    const response = passthroughWrite();
    validateResponseAndRecordMetricWithOverhead(response, 201, 'passthrough write with alternate Id status equals 201', WRITE_DATA_FOR_CM_HANDLE_DELAY_MS, kpiTrendDeclarations.ncmp_write_overhead);
}

export function cmHandleIdSearchNoFilterScenario() {
    const response = executeCmHandleIdSearch('no-filter');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID no-filter search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_id_search_no_filter);
}

export function cmHandleSearchNoFilterScenario() {
    const response = executeCmHandleSearch('no-filter');
    validateResponseAndRecordMetric(response, 200, 'CM handle no-filter search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_search_no_filter);
}

export function cmHandleIdSearchModuleScenario() {
    const response = executeCmHandleIdSearch('module');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID module search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_id_search_module_filter);
}

export function cmHandleSearchModuleScenario() {
    const response = executeCmHandleSearch('module');
    validateResponseAndRecordMetric(response, 200, 'CM handle module search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_search_module_filter);
}

export function cmHandleIdSearchPropertyScenario() {
    const response = executeCmHandleIdSearch('property');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID property search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_id_search_property_filter);
}

export function cmHandleSearchPropertyScenario() {
    const response = executeCmHandleSearch('property');
    validateResponseAndRecordMetric(response, 200, 'CM handle property search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_search_property_filter);
}

export function cmHandleIdSearchCpsPathScenario() {
    const response = executeCmHandleIdSearch('cps-path-for-ready-cm-handles');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID cps path search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_id_search_cps_path_filter);
}

export function cmHandleSearchCpsPathScenario() {
    const response = executeCmHandleSearch('cps-path-for-ready-cm-handles');
    validateResponseAndRecordMetric(response, 200, 'CM handle cps path search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_search_cps_path_filter);
}

export function cmHandleIdSearchTrustLevelScenario() {
    const response = executeCmHandleIdSearch('trust-level');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID trust level search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_id_search_trust_level_filter);
}

export function cmHandleSearchTrustLevelScenario() {
    const response = executeCmHandleSearch('trust-level');
    validateResponseAndRecordMetric(response, 200, 'CM handle trust level search', TOTAL_CM_HANDLES, kpiTrendDeclarations.cm_handle_search_trust_level_filter);
}

export function provMnSReadScenario() {
    const response = provMnSReadOperation();
    validateResponseAndRecordMetric(response, 200, 'ProvMnS Read', TOTAL_CM_HANDLES, kpiTrendDeclarations.provmns_read_overhead);
}

export function provMnSWriteScenario() {
    const response = provMnSWriteOperation();
    validateResponseAndRecordMetric(response, 200, 'ProvMnS Write', TOTAL_CM_HANDLES, kpiTrendDeclarations.provmns_write_overhead);
}

export function legacyBatchProduceScenario() {
    const timestamp1 = (new Date()).toISOString();
    const nextBatchOfAlternateIds = makeRandomBatchOfAlternateIds();
    const response = legacyBatchRead(nextBatchOfAlternateIds);
    check(response, {'data operation batch read status equals 200': (response) => response.status === 200});
    const timestamp2 = (new Date()).toISOString();
    console.debug(`✅ From ${timestamp1} to ${timestamp2} produced ${LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE} messages for legacy batch read`);
}

export function legacyBatchConsumeScenario() {
    const timestamp1 = (new Date()).toISOString();
    try {
        const messages = legacyBatchEventReader.consume({ limit: 220, expectTimeout: true });
        const timestamp2 = (new Date()).toISOString();
        console.debug(`✅ From ${timestamp1} to ${timestamp2} consumed ${messages.length} messages by legacy batch read\``);
        kpiTrendDeclarations.legacy_batch_read.add(messages.length);
    } catch (error) {
        const timestamp2 = (new Date()).toISOString();
        console.error(`❌ From ${timestamp1} to ${timestamp2} Consume error (legacy batch read): ${error.message}`);
    }
}

export function writeDataJobLargeScenario() {
    const response = executeWriteDataJob(100000);
    validateResponseAndRecordMetric(response, 200, 'Large writeDataJob', EXPECTED_WRITE_RESPONSE_COUNT, kpiTrendDeclarations.dcm_write_data_job_large);
}

export function writeDataJobSmallScenario() {
    const response = executeWriteDataJob(100);
    validateResponseAndRecordMetric(response, 200, 'Small writeDataJob', EXPECTED_WRITE_RESPONSE_COUNT, kpiTrendDeclarations.dcm_write_data_job_small);
}

export function produceAvcEventsScenario() {
    sendBatchOfKafkaMessages(500);
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