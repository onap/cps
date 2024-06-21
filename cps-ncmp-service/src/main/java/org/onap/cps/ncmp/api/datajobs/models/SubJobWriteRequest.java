/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

import java.util.Collection;

/**
 * Request data for a write operation by the DMI Plugin.
 *
 * @param dataAcceptType  Define the data response accept type.
 *                        e.g. "application/vnd.3gpp.object-tree-hierarchical+json",
 *                        "application/vnd.3gpp.object-tree-flat+json" etc.
 * @param dataContentType Define the data request content type.
 *                        e.g. "application/3gpp-json-patch+json" etc.
 * @param dataProducerId  Identifier of the data producer.
 *
 * @param data            A collection of outgoing write operations.
 */
public record SubJobWriteRequest (
        String dataAcceptType,
        String dataContentType,
        String dataProducerId,
        Collection<DmiWriteOperation> data) {}