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

package org.onap.cps.ncmp.api.datajobs;

/**
 * Service interface to check the status of a data job.
 * The operations are implemented to interact with a DMI Plugin for data job status retrieval.
 */
public interface DataJobStatusService {

    /**
     * Retrieves the current status of a specific data job.
     *
     * @param authorization     The authorization header from the REST request.
     * @param dmiServiceName    The name of the DMI Service relevant to the data job.
     * @param requestId         The unique identifier for the overall data job request.
     * @param dataProducerJobId The identifier of the data producer job within the DMI system.
     * @param dataProducerId    The ID of the producer registered by DMI, used for operations related to this request.
     *                          This could include alternate IDs or specific identifiers.
     * @return The current status of the data job as a String.
     */
    String getDataJobStatus(final String authorization, final String dmiServiceName,
                            final String requestId,
                            final String dataProducerJobId,
                            final String dataProducerId);
}
