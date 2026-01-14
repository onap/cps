/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe
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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.springframework.http.HttpStatus;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParameterHelper {

    public static final String NO_OP = null;
    private static final String PROVMNS_BASE_PATH = "ProvMnS/v\\d+/";
    private static final String INVALID_PATH_DETAILS_TEMPLATE = "%s not a valid path";
    private static final int PATH_VARIABLES_EXPECTED_LENGTH = 2;
    private static final int REQUEST_FDN_INDEX = 1;

    /**
     * Converts HttpServletRequest to RequestParameters.
     *
     * @param httpServletRequest HttpServletRequest object containing the path
     * @return RequestParameters object containing http method and FDN parameters
     */
    public static RequestParameters extractRequestParameters(final HttpServletRequest httpServletRequest) {
        final String uriPath = (String) httpServletRequest.getAttribute(
            "org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");
        final String[] pathVariables = uriPath.split(PROVMNS_BASE_PATH);
        if (pathVariables.length != PATH_VARIABLES_EXPECTED_LENGTH) {
            throw createProvMnSException(httpServletRequest.getMethod(), uriPath);
        }
        final String fdn = "/" + pathVariables[REQUEST_FDN_INDEX];
        return createRequestParameters(httpServletRequest.getMethod(),
            httpServletRequest.getHeader("Authorization"), fdn);
    }

    /**
     * Create RequestParameters object for PATCH operations.
     *
     * @param pathWithAttributes the path a fdn possibly with containing attributes
     * @param authorization HttpServletRequest object
     * @return RequestParameters object for PATCH operation
     */
    public static RequestParameters createRequestParametersForPatch(
        final String pathWithAttributes, final String authorization) {
        final String fdn = removeTrailingHash(extractFdn(pathWithAttributes));
        return createRequestParameters("PATCH", authorization, fdn);
    }

    /**
     * Extract parent FDN from the given path allowing only className=id pairs.
     *
     * @param path the path to convert
     * @return parent FDN
     */
    public static String extractParentFdn(final String path) {
        return extractFdn(path, 2);
    }

    /**
     * Extract FDN from the given path allowing only className=id pairs.
     *
     * @param path the path to convert
     * @return FDN
     */
    public static String extractFdn(final String path) {
        return extractFdn(path, 1);
    }

    private static String extractFdn(final String path, final int indexFromEnd) {
        final String[] segments = path.split("/");
        int count = 0;
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i].contains("=") && ++count == indexFromEnd) {
                return String.join("/", java.util.Arrays.copyOfRange(segments, 0, i + 1));
            }
        }
        return "";
    }

    private static String removeTrailingHash(final String string) {
        return string.endsWith("#") ? string.substring(0, string.length() - 1) : string;
    }

    private static RequestParameters createRequestParameters(final String httpMethodName,
                                                             final String authorization,
                                                             final String fdn) {
        final int lastSlashIndex = fdn.lastIndexOf('/');
        final String classNameAndId;
        final String uriLdnFirstPart;
        uriLdnFirstPart = fdn.substring(0, lastSlashIndex);
        classNameAndId = fdn.substring(lastSlashIndex + 1);
        final String[] splitClassNameId = classNameAndId.split("=", 2);
        if (splitClassNameId.length != 2) {
            throw createProvMnSException(httpMethodName, fdn);
        }
        final String className = splitClassNameId[0];
        final String id = removeTrailingHash(splitClassNameId[1]);
        return new RequestParameters(httpMethodName, authorization, fdn, uriLdnFirstPart, className, id);
    }

    private static ProvMnSException createProvMnSException(final String httpMethodName, final String uriPath) {
        final String title = String.format(INVALID_PATH_DETAILS_TEMPLATE, uriPath);
        return new ProvMnSException(httpMethodName, HttpStatus.UNPROCESSABLE_ENTITY, title, NO_OP);
    }

}
