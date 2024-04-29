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

export const NCMP_BASE_URL = 'http://localhost:8883';
export const TOTAL_CM_HANDLES = 20000

/*
 * Makes a batch of CM-handle IDs.
 * Given a batchSize=100 and batchNumber=2, it will generate ['ch-201', 'ch-202' ... 'ch-300']
 */
export function makeBatchOfCmHandleIds(batchSize, batchNumber) {
    const batchOfIds = [];
    const startIndex = 1 + batchNumber * batchSize;
    for (let i = 0; i < batchSize; i++) {
        let cmHandleId = 'ch-' + (startIndex + i);
        batchOfIds.push(cmHandleId);
    }
    return batchOfIds;
}

export function getRandomCmHandleId() {
    return 'ch-' + (Math.floor(Math.random() * TOTAL_CM_HANDLES) + 1);
}
