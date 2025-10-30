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
import lombok.Getter;
import lombok.Setter;
import org.onap.cps.ncmp.impl.provmns.exceptions.InvalidPathException;

@Getter
@Setter
public class RequestParameters {

    private String uriLdnFirstPart;
    private String className;
    private String id;

    private static final String PROVMNS_BASE_PATH = "ProvMnS/v\\d+/";

    /**
     * Gets alternate id from combining URI-LDN-First-Part, className and Id.
     *
     * @return String of Alternate Id.
     */
    public String getAlternateId() {
        return uriLdnFirstPart + "/" + className + "=" + id;
    }

    /**
     * Converts HttpServletRequest to RequestParameters.
     *
     * @param httpServletRequest HttpServletRequest object containing the path
     * @return RequestParameters object containing parsed parameters
     */
    public static RequestParameters extractProvMnsRequestParameters(final HttpServletRequest httpServletRequest) {
        final String uriPath = (String) httpServletRequest.getAttribute(
            "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");

        final String[] pathVariables = uriPath.split(PROVMNS_BASE_PATH);
        final int lastSlashIndex = pathVariables[1].lastIndexOf('/');
        if (lastSlashIndex == -1) {
            throw new InvalidPathException(uriPath);
        }
        final RequestParameters requestParameters = new RequestParameters();
        requestParameters.setUriLdnFirstPart("/" + pathVariables[1].substring(0, lastSlashIndex));
        final String classNameAndId = pathVariables[1].substring(lastSlashIndex + 1);

        final String[] splitClassNameId = classNameAndId.split("=", 2);
        if (splitClassNameId.length != 2) {
            throw new InvalidPathException(uriPath);
        }
        requestParameters.setClassName(splitClassNameId[0]);
        requestParameters.setId(splitClassNameId[1]);

        return requestParameters;
    }
}
