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

public interface DataJobStatusService {

    /**
     * Checks the status of a given data job.
     * The incoming parameters are combined into a request, passed southbound to the DMI Plugin.
     *
     * @param dmiServiceName     Name of the relevant DMI Service.
     * @param requestId          Identifier for the overall Datajob
     * @param dataProducerJobId  Identifier of the data producer job.
     * @param dataProducerId     ID of the producer registered by DMI
     *                           for the alernateIDs in the operations in this request.
     *
     * @returns The status of the data job as a String.
     */
    String getDataJobStatus(final String dmiServiceName, final String requestId, final String dataProducerJobId,
                                                                                 final String dataProducerId);
}
