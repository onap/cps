/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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
import { CPS_CORE_BASE_URL, performPostRequest, performDeleteRequest } from './utils.js';

const SCHEMA_SET_YANG_CONTENT = open('../resources/bookstore.yang');

export function createDataspace(dataspaceName) {
    const url = `${CPS_CORE_BASE_URL}/v2/dataspaces?dataspace-name=${dataspaceName}`;
    return performPostRequest(url, null, 'createDataspace');
}

export function deleteDataspace(dataspaceName) {
    const url = `${CPS_CORE_BASE_URL}/v2/dataspaces?dataspace-name=${dataspaceName}`;
    return performDeleteRequest(url, 'deleteDataspace');
}

export function createSchemaSet(dataspaceName, schemaSetName) {
    const url = `${CPS_CORE_BASE_URL}/v2/dataspaces/${dataspaceName}/schema-sets?schema-set-name=${schemaSetName}`;
    // Multipart upload: the form field must be called 'file'; k6 sets the multipart content type itself.
    const payload = {file: http.file(SCHEMA_SET_YANG_CONTENT, 'bookstore.yang')};
    return http.post(url, payload, {tags: {endpoint: 'createSchemaSet'}});
}

export function deleteSchemaSet(dataspaceName, schemaSetName) {
    const url = `${CPS_CORE_BASE_URL}/v2/dataspaces/${dataspaceName}/schema-sets/${schemaSetName}`;
    return performDeleteRequest(url, 'deleteSchemaSet');
}

export function createAnchor(dataspaceName, schemaSetName, anchorName) {
    const url = `${CPS_CORE_BASE_URL}/v2/dataspaces/${dataspaceName}/anchors?schema-set-name=${schemaSetName}&anchor-name=${anchorName}`;
    return performPostRequest(url, null, 'createAnchor');
}

export function deleteAnchor(dataspaceName, anchorName) {
    const url = `${CPS_CORE_BASE_URL}/v2/dataspaces/${dataspaceName}/anchors/${anchorName}`;
    return performDeleteRequest(url, 'deleteAnchor');
}