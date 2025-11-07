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
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.dmi.DmiServiceAuthenticationProperties;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataJobResultServiceImpl implements DataJobResultService {

    private final DmiRestClient dmiRestClient;
    private final DmiServiceAuthenticationProperties dmiServiceAuthenticationProperties;

    @Override
    public String getDataJobResult(final String authorization,
                                   final String dmiServiceName,
                                   final String dataProducerId,
                                   final String dataProducerJobId,
                                   final String destination) {
        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                                           .fixedPathSegment("cmwriteJob")
                                           .fixedPathSegment("dataProducer")
                                           .variablePathSegment("dataProducerId", dataProducerId)
                                           .fixedPathSegment("dataProducerJob")
                                           .variablePathSegment("dataProducerJobId", dataProducerJobId)
                                           .fixedPathSegment("result")
                                           .queryParameter("destination", destination)
                                           .createUrlTemplateParameters(dmiServiceName,
                                               dmiServiceAuthenticationProperties.getDmiBasePath());
        return dmiRestClient.asynchronousDmiDataRequest(urlTemplateParameters, authorization).block();
    }
}
