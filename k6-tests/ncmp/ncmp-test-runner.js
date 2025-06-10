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
import { testConfig, validateResponseAndRecordMetricWithOverhead,
    validateResponseAndRecordMetric, makeCustomSummaryReport, makeBatchOfCmHandleIds, makeRandomBatchOfAlternateIds,
    TOTAL_CM_HANDLES, READ_DATA_FOR_CM_HANDLE_DELAY_MS, WRITE_DATA_FOR_CM_HANDLE_DELAY_MS,
    LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE, REGISTRATION_BATCH_SIZE,
    KAFKA_BOOTSTRAP_SERVERS, LEGACY_BATCH_TOPIC_NAME, CONTAINER_UP_TIME_IN_SECONDS
} from './common/utils.js';
import { createCmHandles, deleteCmHandles, waitForAllCmHandlesToBeReady } from './common/cmhandle-crud.js';
import { executeCmHandleSearch, executeCmHandleIdSearch } from './common/search-base.js';
import { passthroughRead, passthroughWrite, legacyBatchRead } from './common/passthrough-crud.js';
import { sendBatchOfKafkaMessages } from './common/produce-avc-event.js';
import { executeWriteDataJob } from "./common/write-data-job.js";

#METRICS-TRENDS-PLACE-HOLDER#

const EXPECTED_WRITE_RESPONSE_COUNT = 1;

export const legacyBatchEventReader = new Reader({
    brokers: [KAFKA_BOOTSTRAP_SERVERS],
    topic: LEGACY_BATCH_TOPIC_NAME,
});

export const options = {
    setupTimeout: '30m',
    teardownTimeout: '20m',
    scenarios: testConfig.scenarios,
    thresholds: testConfig.thresholds,
};

export function setup() {
    const startTimeInMillis = Date.now();

    const TOTAL_BATCHES = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < TOTAL_BATCHES; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = createCmHandles(nextBatchOfCmHandleIds);
        check(response, { 'create CM-handles status equals 200': (response) => response.status === 200 });
    }

    waitForAllCmHandlesToBeReady();

    const endTimeInMillis = Date.now();
    const totalRegistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    cmHandlesCreatedTrend.add(TOTAL_CM_HANDLES / totalRegistrationTimeInSeconds);
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
        check(response, { 'delete CM-handles status equals 200': (response) => response.status === 200 });
    }

    const endTimeInMillis = Date.now();
    const totalDeregistrationTimeInSeconds = (endTimeInMillis - startTimeInMillis) / 1000.0;

    cmHandlesDeletedTrend.add(DEREGISTERED_CM_HANDLES / totalDeregistrationTimeInSeconds);

    sleep(CONTAINER_UP_TIME_IN_SECONDS);
}

export function passthroughReadAltIdScenario() {
    const response = passthroughRead();
    validateResponseAndRecordMetricWithOverhead(response, 200, 'passthrough read with alternate Id status equals 200', READ_DATA_FOR_CM_HANDLE_DELAY_MS, ncmpReadOverheadTrend);
}

export function passthroughWriteAltIdScenario() {
    const response = passthroughWrite();
    validateResponseAndRecordMetricWithOverhead(response, 201, 'passthrough write with alternate Id status equals 201', WRITE_DATA_FOR_CM_HANDLE_DELAY_MS, ncmpWriteOverheadTrend);
}

export function cmHandleIdSearchNoFilterScenario() {
    const response = executeCmHandleIdSearch('no-filter');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID no-filter search', TOTAL_CM_HANDLES, cmHandleIdSearchNoFilterTrend);
}

export function cmHandleSearchNoFilterScenario() {
    const response = executeCmHandleSearch('no-filter');
    validateResponseAndRecordMetric(response, 200, 'CM handle no-filter search', TOTAL_CM_HANDLES, cmHandleSearchNoFilterTrend);
}

export function cmHandleIdSearchModuleScenario() {
    const response = executeCmHandleIdSearch('module');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID module search', TOTAL_CM_HANDLES, cmHandleIdSearchModuleFilterTrend);
}

export function cmHandleSearchModuleScenario() {
    const response = executeCmHandleSearch('module');
    validateResponseAndRecordMetric(response, 200, 'CM handle module search', TOTAL_CM_HANDLES, cmHandleSearchModuleFilterTrend);
}

export function cmHandleIdSearchPropertyScenario() {
    const response = executeCmHandleIdSearch('property');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID property search', TOTAL_CM_HANDLES, cmHandleIdSearchPropertyFilterTrend);
}

export function cmHandleSearchPropertyScenario() {
    const response = executeCmHandleSearch('property');
    validateResponseAndRecordMetric(response, 200, 'CM handle property search', TOTAL_CM_HANDLES, cmHandleSearchPropertyFilterTrend);
}

export function cmHandleIdSearchCpsPathScenario() {
    const response = executeCmHandleIdSearch('cps-path-for-ready-cm-handles');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID cps path search', TOTAL_CM_HANDLES, cmHandleIdSearchCpsPathFilterTrend);
}

export function cmHandleSearchCpsPathScenario() {
    const response = executeCmHandleSearch('cps-path-for-ready-cm-handles');
    validateResponseAndRecordMetric(response, 200, 'CM handle cps path search', TOTAL_CM_HANDLES, cmHandleSearchCpsPathFilterTrend);
}

export function cmHandleIdSearchTrustLevelScenario() {
    const response = executeCmHandleIdSearch('trust-level');
    validateResponseAndRecordMetric(response, 200, 'CM handle ID trust level search', TOTAL_CM_HANDLES, cmHandleIdSearchTrustLevelFilterTrend);
}

export function cmHandleSearchTrustLevelScenario() {
    const response = executeCmHandleSearch('trust-level');
    validateResponseAndRecordMetric(response, 200, 'CM handle trust level search', TOTAL_CM_HANDLES, cmHandleSearchTrustLevelFilterTrend);
}

export function legacyBatchProduceScenario() {
    const nextBatchOfAlternateIds = makeRandomBatchOfAlternateIds();
    const response = legacyBatchRead(nextBatchOfAlternateIds);
    check(response, {'data operation batch read status equals 200': (response) => response.status === 200});
}

export function writeDataJobLargeScenario() {
    const response = executeWriteDataJob(100000);
    validateResponseAndRecordMetric(response, 200, 'Large writeDataJob', EXPECTED_WRITE_RESPONSE_COUNT, dcmWriteDataJobLargeTrend);
}

export function writeDataJobSmallScenario() {
    const response = executeWriteDataJob(100);
    validateResponseAndRecordMetric(response, 200, 'Small writeDataJob', EXPECTED_WRITE_RESPONSE_COUNT, dcmWriteDataJobSmallTrend);
}

export function produceAvcEventsScenario() {
    sendBatchOfKafkaMessages(500);
}

export function legacyBatchConsumeScenario() {
    // calculate total messages 15 minutes times 60 seconds times
    const TOTAL_MESSAGES_TO_CONSUME = 15 * 60 * LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE;
    console.log("📥 [legacy batch consume scenario] Starting consumption of", TOTAL_MESSAGES_TO_CONSUME, "messages...");

    try {
        let messagesConsumed = 0;
        const startTime = Date.now();

        while (messagesConsumed < TOTAL_MESSAGES_TO_CONSUME) {
            try {
                const messages = legacyBatchEventReader.consume({
                    limit: LEGACY_BATCH_THROUGHPUT_TEST_BATCH_SIZE,
                    timeout: 30000,
                });

                if (messages.length > 0) {
                    messagesConsumed += messages.length;
                    console.debug(`✅ Consumed ${messages.length} messages by legacy batch read (total: ${messagesConsumed}/${TOTAL_MESSAGES_TO_CONSUME})`);
                } else {
                    console.warn("⚠️ No messages received by legacy batch read.");
                }
            } catch (err) {
                console.error(`❌ Consume error (legacy batch read): ${err.message}`);
            }
        }

        const endTime = Date.now();
        const timeToConsumeMessagesInSeconds = (endTime - startTime) / 1000.0;

        if (messagesConsumed > 0) {
            legacyBatchReadTrend.add(messagesConsumed / timeToConsumeMessagesInSeconds);
            console.log(`🏁 Finished (legacy batch read): Consumed ${messagesConsumed} messages in ${timeToConsumeMessagesInSeconds.toFixed(2)}s.`);
        } else {
            legacyBatchReadCmhandlesPerSecondTrend.add(0);
            console.error("⚠️ No messages consumed by legacy read batch.");
        }
    } catch (error) {
        legacyBatchReadTrend.add(0);
        console.error("💥 Legacy batch read scenario failed:", error.message);
    }
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
