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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.DataJobStatusService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataJobStatusServiceImpl implements DataJobStatusService {

    private final DmiSubJobStatusRequestHandler dmiSubJobStatusRequestHandler;

    @Override
    public String getDataJobStatus(final String dmiServiceName, final String requestId, final String dataProducerJobId,
                                   final String dataProducerId) {
        return dmiSubJobStatusRequestHandler.getDataJobStatusFromDmi(dmiServiceName, requestId, dataProducerJobId,
                                                                                                dataProducerId);
    }
}
