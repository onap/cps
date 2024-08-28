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
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';
import {
    TOTAL_CM_HANDLES, CONTENT_TYPE_JSON_PARAM,
    NCMP_BASE_URL, LEGACY_BATCH_TOPIC_NAME
} from './utils.js';


export function passthroughRead(useAlternateId) {
    const cmHandleReference = getRandomCmHandleReference(useAlternateId);
    const resourceIdentifier = 'my-resource-identifier';
    const includeDescendants = true;
    const datastoreName = 'ncmp-datastore:passthrough-operational';
    const url = `${NCMP_BASE_URL}/ncmp/v1/ch/${cmHandleReference}/data/ds/${datastoreName}?resourceIdentifier=${resourceIdentifier}&include-descendants=${includeDescendants}`
    const response = http.get(url);
    return response;
}

export function passthroughWrite(useAlternateId) {
    const cmHandleReference = getRandomCmHandleReference(useAlternateId);
    const resourceIdentifier = 'my-resource-identifier';
    const datastoreName = 'ncmp-datastore:passthrough-running';
    const url = `${NCMP_BASE_URL}/ncmp/v1/ch/${cmHandleReference}/data/ds/${datastoreName}?resourceIdentifier=${resourceIdentifier}`
    const body = `{"neType": "BaseStation"}`
    const response = http.post(url, JSON.stringify(body), CONTENT_TYPE_JSON_PARAM);
    return response;
}

export function batchRead(cmHandleIds) {
    const url = `${NCMP_BASE_URL}/ncmp/v1/data?topic=${LEGACY_BATCH_TOPIC_NAME}`
    const  payload = {
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
    const response = http.post(url, JSON.stringify(payload), CONTENT_TYPE_JSON_PARAM);
    return response;
}

function getRandomCmHandleReference(useAlternateId) {
    const prefix = useAlternateId ? 'alt' : 'ch';
    return `${prefix}-${randomIntBetween(1, TOTAL_CM_HANDLES)}`;
}
