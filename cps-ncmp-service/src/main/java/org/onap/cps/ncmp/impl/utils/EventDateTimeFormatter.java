/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.utils;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;

public interface EventDateTimeFormatter {

    String ISO_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    DateTimeFormatter ISO_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIMESTAMP_PATTERN);

    /**
     * Gets current date time.
     *
     * @return the current date time
     */
    static String getCurrentIsoFormattedDateTime() {
        return ZonedDateTime.now().format(ISO_TIMESTAMP_FORMATTER);
    }

    static OffsetDateTime toIsoOffsetDateTime(final String dateTimestampAsString) {
        return StringUtils.isNotBlank(dateTimestampAsString)
                ? OffsetDateTime.parse(dateTimestampAsString, ISO_TIMESTAMP_FORMATTER) : null;
    }
}
