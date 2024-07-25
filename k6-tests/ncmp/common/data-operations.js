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
    NCMP_BASE_URL,
    CONTENT_TYPE_JSON_PARAM,
} from './utils.js';

export function batchRead(cmHandleIds) {
    const topic = 'topic-batch-read'
    const url = `${NCMP_BASE_URL}/ncmp/v1/data?topic=${topic}`
    const  payload = {
        "operations": [
            {
                "resourceIdentifier": "parent/child",
                "targetIds": cmHandleIds.map(cmHandleId => cmHandleId,),
                "datastore": "ncmp-datastore:passthrough-operational",
                "options": "(fields=schemas/schema)",
                "operationId": "12",
                "operation": "read"
            }
        ]
    };
    console.log('sending the request')
    const response = http.post(url, JSON.stringify(payload), CONTENT_TYPE_JSON_PARAM);
    return response;
}