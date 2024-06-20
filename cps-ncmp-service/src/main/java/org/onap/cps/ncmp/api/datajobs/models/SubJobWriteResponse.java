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

/**
 * Request data for a write operation towards DMI Plugin.
 *
 * @param subJobId        Identifier of the sub-job from DMI.
 * @param dmiServiceName  The provided name of the DMI service from the request.
 * @param dataProducerId  Identifier of the data producer.
 */
public record SubJobWriteResponse(String subJobId, String dmiServiceName, String dataProducerId) {}