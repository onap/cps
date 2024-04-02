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

package org.onap.cps.ncmp.api.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.DataJobService;
import org.onap.cps.ncmp.api.models.datajob.DataJobReadRequest;
import org.onap.cps.ncmp.api.models.datajob.DataJobWriteRequest;
import org.onap.cps.ncmp.api.models.datajob.OutputParameters;
import org.onap.cps.ncmp.api.models.datajob.Response;

@Slf4j
public class DataJobServiceImpl implements DataJobService {

    @Override
    public Response readDataJob(final OutputParameters outputParameters, final String dataJobId,
                                final DataJobReadRequest dataJobReadRequest) {
        // Todo
        // As all the arguments of Response is mandatory, we are passing empty status uri and results uri with empty
        // status until CPS-2142 is closed.
        return new Response(dataJobId, "", toUri(""), toUri(""));
    }

    @Override
    public Response writeDataJob(final OutputParameters outputParameters, final String dataJobId,
                                 final DataJobWriteRequest dataJobWriteRequest, final Map<String, String> metadata) {
        // Todo
        // As all the arguments of Response is mandatory, we are passing empty status uri and results uri with empty
        // status until CPS-2142 is closed.
        return new Response(dataJobId, "", toUri(""), toUri(""));
    }

    private URI toUri(final String dataJobResponseUri) {
        try {
            return new URI(dataJobResponseUri);
        } catch (final URISyntaxException e) {
            log.error("Invalid response uri: {}", dataJobResponseUri);
        }
        return null;
    }

}
