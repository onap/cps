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

import {
    performPostRequest,
    performGetRequest,
    NCMP_BASE_URL,
    LEGACY_BATCH_TOPIC_NAME,
    getRandomAlternateId,
} from './utils.js';

export function passthroughRead() {
    const randomAlternateId  = getRandomAlternateId();
    const resourceIdentifier = 'ManagedElement=NRNode1/GNBDUFunction=1';
    const datastoreName = 'ncmp-datastore:passthrough-operational';
    const includeDescendants = true;
    const url = generatePassthroughUrl(randomAlternateId , datastoreName, resourceIdentifier, includeDescendants);
    return performGetRequest(url, 'passthroughRead');
}

export function passthroughWrite() {
    const randomAlternateId  = getRandomAlternateId();
    const resourceIdentifier = 'ManagedElement=NRNode1/GNBDUFunction=1';
    const datastoreName = 'ncmp-datastore:passthrough-running';
    const includeDescendants = false;
    const url = generatePassthroughUrl(randomAlternateId , datastoreName, resourceIdentifier, includeDescendants);
    const payload = JSON.stringify({
        "id": "123",
        "attributes": {"userLabel": "test"}
    });
    return performPostRequest(url, payload, 'passthroughWrite');
}

export function legacyBatchRead(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmp/v1/data?topic=${LEGACY_BATCH_TOPIC_NAME}`
    const payload = JSON.stringify({
        "operations": [
            {
                "resourceIdentifier": "ManagedElement=NRNode1/GNBDUFunction=1",
                "targetIds": cmHandleIds,
                "datastore": "ncmp-datastore:passthrough-operational",
                "options": "(fields=NRCellDU/attributes/cellLocalId)",
                "operationId": "12",
                "operation": "read"
            }
        ]
    });
    return performPostRequest(url, payload, 'batchRead');
}

function generatePassthroughUrl(alternateId, datastoreName, resourceIdentifier, includeDescendants) {
    const encodedAlternateId = encodeURIComponent(alternateId);
    const descendantsParam = includeDescendants ? `&include-descendants=${includeDescendants}` : '';
    return `${NCMP_BASE_URL}/ncmp/v1/ch/${encodedAlternateId}/data/ds/${datastoreName}?resourceIdentifier=${resourceIdentifier}${descendantsParam}`;
}