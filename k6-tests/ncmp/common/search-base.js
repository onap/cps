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
import { NCMP_BASE_URL, CONTENT_TYPE_JSON_PARAM } from './utils.js';

const SEARCH_PARAMETERS_PER_SCENARIO = {
    'module': {
        'cmHandleQueryParameters': [
            {
                'conditionName': 'hasAllModules',
                'conditionParameters': [{'moduleName': 'ietf-yang-types-1'}]
            }
        ]
    },
    'readyCmHandles': {
        'cmHandleQueryParameters': [
            {
                'conditionName': 'cmHandleWithCpsPath',
                'conditionParameters': [{'cpsPath': '//state[@cm-handle-state="READY"]'}]
            }
        ]
    }
};

export function executeCmHandleSearch(scenario) {
    return executeSearchRequest('searches', scenario);
}

export function executeCmHandleIdSearch(scenario) {
    return executeSearchRequest('id-searches', scenario);
}

function executeSearchRequest(searchType, scenario) {
    const searchParameters = SEARCH_PARAMETERS_PER_SCENARIO[scenario];
    const payload = JSON.stringify(searchParameters);
    const url = `${NCMP_BASE_URL}/ncmp/v1/ch/${searchType}`;
    const response = http.post(url, payload, CONTENT_TYPE_JSON_PARAM);
    return response;
}
