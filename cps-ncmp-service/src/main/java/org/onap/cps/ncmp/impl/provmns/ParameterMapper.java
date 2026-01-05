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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParameterMapper {

    private static final String PROVMNS_BASE_PATH = "ProvMnS/v\\d+/";
    private static final String INVALID_PATH_DETAILS_TEMPLATE = "%s not a valid path";
    private static final String NO_OP = null;
    private static final int PATH_VARIABLES_EXPECTED_LENGTH = 2;
    private static final int OBJECT_INSTANCE_INDEX = 1;

    /**
     * Converts HttpServletRequest to RequestParameters.
     *
     * @param httpServletRequest HttpServletRequest object containing the path
     * @return RequestParameters object containing http method and parsed parameters
     */
    public RequestParameters extractRequestParameters(final HttpServletRequest httpServletRequest) {
        final String uriPath = (String) httpServletRequest.getAttribute(
            "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");
        final String[] pathVariables = uriPath.split(PROVMNS_BASE_PATH);
        if (pathVariables.length != PATH_VARIABLES_EXPECTED_LENGTH) {
            throw createProvMnSException(httpServletRequest.getMethod(), uriPath);
        }
        final int lastSlashIndex = pathVariables[1].lastIndexOf('/');
        final RequestParameters requestParameters = new RequestParameters();
        requestParameters.setHttpMethodName(httpServletRequest.getMethod());
        final String classNameAndId;
        if (lastSlashIndex < 0) {
            requestParameters.setUriLdnFirstPart("");
            classNameAndId = pathVariables[OBJECT_INSTANCE_INDEX];
        } else {
            final String uriLdnFirstPart = "/" + pathVariables[OBJECT_INSTANCE_INDEX].substring(0, lastSlashIndex);
            requestParameters.setUriLdnFirstPart(uriLdnFirstPart);
            classNameAndId = pathVariables[OBJECT_INSTANCE_INDEX].substring(lastSlashIndex + 1);
        }
        final String[] splitClassNameId = classNameAndId.split("=", 2);
        if (splitClassNameId.length != 2) {
            throw createProvMnSException(httpServletRequest.getMethod(), uriPath);
        }
        requestParameters.setClassName(splitClassNameId[0]);
        requestParameters.setId(splitClassNameId[1]);
        return requestParameters;
    }

    private ProvMnSException createProvMnSException(final String httpMethodName, final String uriPath) {
        final String title = String.format(INVALID_PATH_DETAILS_TEMPLATE, uriPath);
        return new ProvMnSException(httpMethodName, HttpStatus.UNPROCESSABLE_ENTITY, title, NO_OP);
    }

}
