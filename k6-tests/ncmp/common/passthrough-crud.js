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

import http from 'k6/http';
import {
    CONTENT_TYPE_JSON_PARAM,
    getRandomCmHandleId,
    NCMP_BASE_URL,
    TOPIC_DATA_OPERATIONS_BATCH_READ
} from './utils.js';

// Helper function to generate URL for passthrough operations
function generatePassthroughUrl(cmHandleId, datastoreName, resourceIdentifier, includeDescendants = false, alt = false) {
    const cmHandlePrefix = alt ? `alt-${cmHandleId}` : cmHandleId;
    const descendantsParam = includeDescendants ? `&include-descendants=${includeDescendants}` : '';
    return `${NCMP_BASE_URL}/ncmp/v1/ch/${cmHandlePrefix}/data/ds/${datastoreName}?resourceIdentifier=${resourceIdentifier}${descendantsParam}`;
}

// Helper function to make a GET request with tags
function performGetRequest(url, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };
    return http.get(url, {tags: metricTags});
}

// Helper function to make a POST request with tags
function performPostRequest(url, payload, metricTag) {
    const metricTags = {
        endpoint: metricTag
    };

    return http.post(url, JSON.stringify(payload), {
        headers: CONTENT_TYPE_JSON_PARAM,
        tags: metricTags
    });
}

export function passthroughRead() {
    const cmHandleId = getRandomCmHandleId();
    const resourceIdentifier = 'my-resource-identifier';
    const datastoreName = 'ncmp-datastore:passthrough-operational';
    const includeDescendants = true;
    const url = generatePassthroughUrl(cmHandleId, datastoreName, resourceIdentifier, includeDescendants);
    return performGetRequest(url, 'passthroughRead', cmHandleId);
}

export function passthroughReadWithAltId() {
    const cmHandleId = getRandomCmHandleId();
    const resourceIdentifier = 'my-resource-identifier';
    const datastoreName = 'ncmp-datastore:passthrough-operational';
    const includeDescendants = true;
    const url = generatePassthroughUrl(cmHandleId, datastoreName, resourceIdentifier, includeDescendants, true);
    return performGetRequest(url, 'passthroughReadWithAltId', `alt-${cmHandleId}`);
}

export function passthroughWrite() {
    const cmHandleId = getRandomCmHandleId();
    const resourceIdentifier = 'my-resource-identifier';
    const datastoreName = 'ncmp-datastore:passthrough-running';
    const url = generatePassthroughUrl(cmHandleId, datastoreName, resourceIdentifier);
    const body = {"neType": "BaseStation"};
    return performPostRequest(url, body, 'passthroughWrite', cmHandleId);
}

export function batchRead(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmp/v1/data?topic=${TOPIC_DATA_OPERATIONS_BATCH_READ}`;
    const payload = {
        "operations": [
            {
                "resourceIdentifier": "parent/child",
                "targetIds": cmHandleIds,
                "datastore": "ncmp-datastore:passthrough-operational",
                "options": "(fields=schemas/schema)",
                "operationId": "12",
                "operation": "read"
            }
        ]
    };
    return performPostRequest(url, payload, 'batchRead');
}