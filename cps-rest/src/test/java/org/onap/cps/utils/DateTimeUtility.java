/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.utils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

public interface DateTimeUtility {

    String ISO_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    DateTimeFormatter ISO_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIMESTAMP_PATTERN);

    static OffsetDateTime toOffsetDateTime(String datetTimestampAsString) {
        return ! StringUtils.hasLength(datetTimestampAsString)
            ? null : OffsetDateTime.parse(datetTimestampAsString, ISO_TIMESTAMP_FORMATTER);
    }

    static String toString(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? ISO_TIMESTAMP_FORMATTER.format(offsetDateTime) : null;
    }
}
