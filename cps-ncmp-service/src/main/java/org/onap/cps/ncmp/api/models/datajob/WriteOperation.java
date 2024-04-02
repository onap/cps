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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.models.datajob;

/**
 * Holds information of write data job operation.
 * based on <a href="https://www.etsi.org/deliver/etsi_ts/128500_128599/128532/16.04.00_60/ts_128532v160400p.pdf">ETSI TS 128 532 V16.4.0 (2020-08)</a>
 *
 * @param path        Identifier of a managed object (MO) on a network element. Defines the resource on which operation
 *                    is executed. Typically, is Fully Distinguished Name (FDN).
 * @param op          Describes the operation to execute.  The value can be as below:
 *                    - "add": creates a new MO with the id and attributes given in the value.
 *                    - "replace":  replaces the value of an attribute specified in the path.
 *                    - "remove": removes the MO given in the path, value is not applicable.
 *                    - "action": executes an MO action in the path with the input parameters in the value.
 * @param operationId Unique identifier of the operation within the request.
 * @param value       - n/a if op == "remove"
 *                    - Resource object if op == "add"
 *                    - Object if op == "replace"
 *                    - ActionParameters object if op == "action"
 */
public record WriteOperation(String path, String op, String operationId, Object value) {}
