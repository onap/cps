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

package org.onap.cps.ncmp.impl.datajobs;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.DataJobService;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;

@Slf4j
public class DataJobServiceImpl implements DataJobService {

    @Override
    public void readDataJob(final String dataJobId, final DataJobMetadata dataJobMetadata,
                            final DataJobReadRequest dataJobReadRequest) {
        log.info("data job id for read operation is: {}", dataJobId);
    }

    @Override
    public void writeDataJob(final String dataJobId, final DataJobMetadata dataJobMetadata,
                             final DataJobWriteRequest dataJobWriteRequest) {
        log.info("data job id for write operation is: {}", dataJobId);
    }
}
