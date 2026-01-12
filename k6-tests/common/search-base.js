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

import {performPostRequest, NCMP_BASE_URL} from './utils.js';

export function executeCmHandleSearch(scenario) {
    return executeSearchRequest('searches', scenario);
}

export function executeCmHandleIdSearch(scenario) {
    return executeSearchRequest('id-searches', scenario);
}

function executeSearchRequest(searchType, scenario) {
    const searchParameters = SEARCH_PARAMETERS_PER_SCENARIO[scenario];
    const payload = JSON.stringify(searchParameters);
    const url = `${NCMP_BASE_URL}/ncmp/v1/ch/${searchType}?outputAlternateId=true`;
    return performPostRequest(url, payload, searchType);
}

const SEARCH_PARAMETERS_PER_SCENARIO = {
    "no-filter": {
        "cmHandleQueryParameters": []
    },
    "module": {
        "cmHandleQueryParameters": [
            {
                "conditionName": "hasAllModules",
                "conditionParameters": [{"moduleName": "module100"}]
            }
        ]
    },
    "property": {
        "cmHandleQueryParameters": [
            {
                "conditionName": "hasAllProperties",
                "conditionParameters": [{"systemName": "ncmp"}]
            }
        ]
    },
    "trust-level": {
        "cmHandleQueryParameters": [
            {
                "conditionName": "cmHandleWithTrustLevel",
                "conditionParameters": [ {"trustLevel": "COMPLETE"} ]
            }
        ]
    },
    "cps-path-for-ready-cm-handles": {
        "cmHandleQueryParameters": [
            {
                "conditionName": "cmHandleWithCpsPath",
                "conditionParameters": [{"cpsPath": "//state[@cm-handle-state='READY']"}]
            }
        ]
    }
};