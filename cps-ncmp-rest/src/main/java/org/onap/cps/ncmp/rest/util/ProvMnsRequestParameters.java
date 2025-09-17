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

package org.onap.cps.ncmp.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Setter;
import org.onap.cps.ncmp.rest.provmns.exception.InvalidPathException;

@Getter
@Setter
public class ProvMnsRequestParameters {

    private String uriLdnFirstPart;
    private String className;
    private String id;

    private static final String PROVMNS_BASE_PATH = "ProvMnS/v\\d+/";

    /**
     * Converts HttpServletRequest to ProvMnsRequestParameters.
     *
     * @param httpServletRequest HttpServletRequest object containing the path
     * @return ProvMnsRequestParameters object containing parsed parameters
     */
    public static ProvMnsRequestParameters toProvMnsRequestParameters(final HttpServletRequest httpServletRequest) {
        final String uriPath = (String) httpServletRequest.getAttribute(
            "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");

        final String[] pathVariables = uriPath.split(PROVMNS_BASE_PATH);
        final int lastSlashIndex = pathVariables[1].lastIndexOf('/');
        if (lastSlashIndex == -1) {
            throw new InvalidPathException(uriPath);
        }
        final ProvMnsRequestParameters provMnsRequestParameters = new ProvMnsRequestParameters();
        provMnsRequestParameters.setUriLdnFirstPart(pathVariables[1].substring(0, lastSlashIndex));
        final String classNameAndId = pathVariables[1].substring(lastSlashIndex + 1);

        final String[] splitClassNameId = classNameAndId.split("=", 2);
        if (splitClassNameId.length != 2) {
            throw new InvalidPathException(uriPath);
        }
        provMnsRequestParameters.setClassName(splitClassNameId[0]);
        provMnsRequestParameters.setId(splitClassNameId[1]);

        return provMnsRequestParameters;
    }
}
