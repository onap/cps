/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.impl.provmns;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParameterMapper {

    private static final String PROVMNS_BASE_PATH = "ProvMnS/v\\d+/";
    private static final String INVALID_PATH_DETAILS_FORMAT = "%s not a valid path";

    /**
     * Converts HttpServletRequest to RequestPathParameters.
     *
     * @param httpServletRequest HttpServletRequest object containing the path
     * @return RequestPathParameters object containing parsed parameters
     */
    public RequestPathParameters extractRequestParameters(final HttpServletRequest httpServletRequest) {
        final String uriPath = (String) httpServletRequest.getAttribute(
            "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");

        final String[] pathVariables = uriPath.split(PROVMNS_BASE_PATH);
        final int lastSlashIndex = pathVariables[1].lastIndexOf('/');
        final RequestPathParameters requestPathParameters = new RequestPathParameters();
        
        final String classNameAndId;
        if (lastSlashIndex < 0) {
            requestPathParameters.setUriLdnFirstPart("");
            classNameAndId = pathVariables[1];
        } else {
            requestPathParameters.setUriLdnFirstPart("/" + pathVariables[1].substring(0, lastSlashIndex));
            classNameAndId = pathVariables[1].substring(lastSlashIndex + 1);
        }

        final String[] splitClassNameId = classNameAndId.split("=", 2);
        if (splitClassNameId.length != 2) {
            throw new ProvMnSException("not a valid path", String.format(INVALID_PATH_DETAILS_FORMAT, uriPath));
        }
        requestPathParameters.setClassName(splitClassNameId[0]);
        requestPathParameters.setId(splitClassNameId[1]);

        return requestPathParameters;
    }
}
