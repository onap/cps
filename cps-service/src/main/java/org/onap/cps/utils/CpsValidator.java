/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.utils;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.DataValidationException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CpsValidator {

    private static final char[] UNSUPPORTED_NAME_CHARACTERS = "!\" #$%&'()*+,./\\:;<=>?@[]^`{|}~".toCharArray();
    private static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([._-](?![._-])|"
            + "[a-zA-Z0-9]){0,120}[a-zA-Z0-9]$");

    /**
     * Validate characters in names within cps.
     * @param names names of data to be validated
     */
    public static void validateNameCharacters(final String... names) {
        for (final String name : names) {
            final  Collection<Character> charactersOfName = Lists.charactersOf(name);
            for (final char unsupportedCharacter : UNSUPPORTED_NAME_CHARACTERS) {
                if (charactersOfName.contains(unsupportedCharacter)) {
                    throw new DataValidationException("Name or ID Validation Error.",
                        name + " invalid token encountered at position " + (name.indexOf(unsupportedCharacter) + 1));
                }
            }
        }
    }

    /**
     * Validate kafka topic name pattern.
     * @param topicName name of the topic to be validated
     */
    public static boolean validateTopicName(final String topicName) {
        return topicName != null && TOPIC_NAME_PATTERN.matcher(topicName).matches();
    }
}
