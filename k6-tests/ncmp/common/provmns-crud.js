/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
    performGetRequest, performPatchRequest, NCMP_BASE_URL, getRandomAlternateId
} from './utils.js';

export function provMnSReadOperation() {
    const randomAlternateId  = getRandomAlternateId();
    const url = `${NCMP_BASE_URL}/ProvMnS/v1` + randomAlternateId;
    return performGetRequest(url, 'provMnSRead');
}

export function provMnSWriteOperation() {
    const randomAlternateId  = getRandomAlternateId();
    const url = `${NCMP_BASE_URL}/ProvMnS/v1` + randomAlternateId;
    const payload = JSON.stringify(
        [
            {
                "op": "replace",
                "path": "/parent=id1/child=id2/grandchild=id3#/attributes/plmnId/mcc",
                "value": "1234"
            }
        ]);
    return performPatchRequest(url, payload, 'provMnSWrite');
}