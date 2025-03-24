/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.datajobs.models;

/**
 * Describes the write data job operation to be forwarded to dmi.
 *
 * @param path               Identifier of a managed object (MO) on a network element. Defines the resource on which
 *                           operation is executed. Typically, is Fully Distinguished Name (FDN).
 * @param op                 Describes the operation to execute.  The value can be as below:
 *                           e.g. "add", "replace", "remove", "action" etc.
 * @param moduleSetTag       The module set tag of the CM Handle.
 * @param value              The value to be written depends on the type of operation.
 * @param operationId        Unique identifier of the operation within the request.
 */
public record DmiWriteOperation(
        String path,
        String op,
        String moduleSetTag,
        Object value,
        String operationId) {}
