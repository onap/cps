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

package org.onap.cps.ncmp.api;

import java.util.Map;
import org.onap.cps.ncmp.api.models.datajob.DataJobReadRequest;
import org.onap.cps.ncmp.api.models.datajob.DataJobWriteRequest;
import org.onap.cps.ncmp.api.models.datajob.OutputParameters;
import org.onap.cps.ncmp.api.models.datajob.Response;


public interface DataJobService {

    /**
     * process read data job operation.
     *
     * @param outputParameters   data job request headers
     * @param dataJobId          Unique identifier of the job within the request
     * @param dataJobReadRequest read data job request
     * @return job current status
     */
    Response readDataJob(OutputParameters outputParameters, String dataJobId, DataJobReadRequest dataJobReadRequest);

    /**
     * process write data job.
     *
     * @param outputParameters    data job request headers
     * @param dataJobId           Unique identifier of the job within the request
     * @param dataJobWriteRequest write data job request
     * @param metadata            additional information of data job
     * @return job current status
     */
    Response writeDataJob(OutputParameters outputParameters, String dataJobId, DataJobWriteRequest dataJobWriteRequest,
                          Map<String, String> metadata);
}