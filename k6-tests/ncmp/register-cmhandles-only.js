/*
 *  ============LICENSE_START=======================================================
 *  Copyright 2025 OpenInfra Foundation Europe. All rights reserved.
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
 * To run this script, ensure docker-compose is started, then run this k6 script:
 *   docker-compose -f docker-compose/docker-compose.yml --profile dmi-stub --project-name kpi up --wait
 *   k6 run register-cmhandles-only.js -e TEST_PROFILE=kpi
 * After, the system will be running with 50,000 CM-handles created.
 */

import { check } from 'k6';
import { TOTAL_CM_HANDLES, REGISTRATION_BATCH_SIZE, makeBatchOfCmHandleIds } from './common/utils.js';
import { createCmHandles, waitForAllCmHandlesToBeReady } from './common/cmhandle-crud.js';

/**
 * This function registers CM-handles in batches and waits until all are in READY state.
 * The number of handles to be registered is TOTAL_CM_HANDLES defined in common/utils.js
 */
export default function () {
    const numberOfBatches = Math.ceil(TOTAL_CM_HANDLES / REGISTRATION_BATCH_SIZE);
    for (let batchNumber = 0; batchNumber < numberOfBatches; batchNumber++) {
        const nextBatchOfCmHandleIds = makeBatchOfCmHandleIds(REGISTRATION_BATCH_SIZE, batchNumber);
        const response = createCmHandles(nextBatchOfCmHandleIds);
        check(response, { 'create CM-handles status equals 200': (r) => r.status === 200 });
    }
    waitForAllCmHandlesToBeReady();
}
