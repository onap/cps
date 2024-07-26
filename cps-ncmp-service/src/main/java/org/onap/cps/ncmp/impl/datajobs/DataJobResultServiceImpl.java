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
import org.onap.cps.ncmp.api.datajobs.DataJobResultService;
import org.onap.cps.ncmp.impl.dmi.DmiProperties;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.dmi.DmiServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.dmi.UrlTemplateParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataJobResultServiceImpl implements DataJobResultService {

    private final DmiRestClient dmiRestClient;
    private final DmiProperties dmiProperties;

    @Override
    public Object getDataJobResult(final String authorization,
                                   final String dmiServiceName,
                                   final String requestId,
                                   final String dataProducerJobId,
                                   final String dataProducerId,
                                   final String destination) {
        final UrlTemplateParameters urlTemplateParameters = buildUrlParameters(dmiServiceName,
                                                                               requestId,
                                                                               dataProducerJobId,
                                                                               dataProducerId,
                                                                               destination);
        return dmiRestClient.getDataJobResult(urlTemplateParameters, authorization).block();
    }

    private UrlTemplateParameters buildUrlParameters(final String dmiServiceName,
                                                     final String requestId,
                                                     final String dataProducerJobId,
                                                     final String dataProducerId,
                                                     final String destination) {
        return DmiServiceUrlTemplateBuilder.newInstance()
                                           .fixedPathSegment("dataJob")
                                           .variablePathSegment("requestId", requestId)
                                           .fixedPathSegment("dataProducerJob")
                                           .variablePathSegment("dataProducerJobId", dataProducerJobId)
                                           .fixedPathSegment("result")
                                           .queryParameter("dataProducerId", dataProducerId)
                                           .queryParameter("destination", destination)
                                           .createUrlTemplateParameters(dmiServiceName, dmiProperties.getDmiBasePath());
    }

}
