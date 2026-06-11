/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.dmi;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates DMI plugin URLs to prevent SSRF attacks.
 *
 * <p>When an allowed URL pattern is configured via {@code ncmp.dmi.allowed-url-pattern},
 * only URLs matching that regex are accepted. When no pattern is configured, basic validation
 * ensures the URL uses an http or https scheme and contains a valid host.
 */
@Component
public class DmiPluginUrlValidator {

    private final Pattern allowedUrlPattern;

    /**
     * Constructor.
     *
     * @param allowedUrlPatternString optional regex pattern to restrict allowed DMI plugin URLs.
     *                                If blank, only scheme and host validation is applied.
     */
    public DmiPluginUrlValidator(@Value("${ncmp.dmi.allowed-url-pattern:}") final String allowedUrlPatternString) {
        this.allowedUrlPattern = StringUtils.isNotBlank(allowedUrlPatternString)
            ? Pattern.compile(allowedUrlPatternString)
            : null;
    }

    /**
     * Validates all DMI plugin URL fields in the registration.
     *
     * @param dmiPluginRegistration the registration containing DMI plugin URLs
     * @throws DataValidationException if any URL is invalid
     */
    public void validateDmiPluginUrls(final DmiPluginRegistration dmiPluginRegistration) {
        final Map<String, String> urlFields = Map.of(
            "dmiPlugin", StringUtils.defaultString(dmiPluginRegistration.getDmiPlugin()),
            "dmiDataPlugin", StringUtils.defaultString(dmiPluginRegistration.getDmiDataPlugin()),
            "dmiModelPlugin", StringUtils.defaultString(dmiPluginRegistration.getDmiModelPlugin()),
            "dmiDatajobsReadPlugin", StringUtils.defaultString(dmiPluginRegistration.getDmiDatajobsReadPlugin()),
            "dmiDatajobsWritePlugin", StringUtils.defaultString(dmiPluginRegistration.getDmiDatajobsWritePlugin())
        );
        urlFields.forEach((fieldName, url) -> {
            if (StringUtils.isNotBlank(url) && url.contains("://")) {
                validateUrl(fieldName, url);
            }
        });
    }

    private void validateUrl(final String fieldName, final String url) {
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (final IllegalArgumentException exception) {
            throw new DataValidationException("Invalid DMI plugin URL for " + fieldName,
                "URL is not valid: " + url);
        }
        if (allowedUrlPattern != null) {
            if (!allowedUrlPattern.matcher(url).matches()) {
                throw new DataValidationException("Invalid DMI plugin URL for " + fieldName,
                    "URL does not match the allowed pattern: " + url);
            }
        } else {
            final String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                throw new DataValidationException("Invalid DMI plugin URL for " + fieldName,
                    "Only http or https schemes are allowed, got: " + url);
            }
            if (uri.getHost() == null) {
                throw new DataValidationException("Invalid DMI plugin URL for " + fieldName,
                    "URL must contain a valid host: " + url);
            }
        }
    }
}
