/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import {performPostRequest, NCMP_BASE_URL} from './utils.js';

export function executeWriteDataJob() {
    const authorization = 'Bearer my-authorization-token';
    const dataJobId = 'job-001'; // or get from scenario
    const dataJobRequest = populateDataJobWriteRequests(10_000); // ✅ build it here

    return executeWriteDataJobRequest(authorization, dataJobId, dataJobRequest);
}

function executeWriteDataJobRequest(authorization, dataJobId, dataJobRequest) {
    const payload = JSON.stringify(dataJobRequest);
    const url = `${NCMP_BASE_URL}/ncmp/v1/do-not-use/dataJobs/${dataJobId}/write`;
    return performPostRequest(url, payload, 'WriteDataJob');
}

function populateDataJobWriteRequests(numberOfWriteOperations) {
    const writeOperations = [];

    for (let i = 1; i <= numberOfWriteOperations; i++) {
        const basePath = `/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${i}/ManagedElement=MyManagedElement${i}`;

        writeOperations.push({
            path: `${basePath}/SomeChild=child-1`,
            op: 'add',
            operationId: '1',
            value: 'some-value-one'
        });

        writeOperations.push({
            path: `${basePath}/SomeChild=child-2`,
            op: 'merge',
            operationId: '1',
            value: 'some-value-two'
        });

        writeOperations.push({
            path: basePath,
            op: 'remove',
            operationId: '1',
            value: 'some-value-three'
        });
    }

    return {
        data: writeOperations
    };
}

