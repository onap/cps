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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.DataValidationException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtils {

    private static final String INVALID_NAME_AND_IDENTIFIER_CHARACTERS = "!“” #$%&‘()*+,./\\:;<=>?@[]^`{|}~";

    /**
     * Validate Function Names/ID's.
     * @param names names
     */
    public static void validateFunctionIds(final String... names) {
        for (final String name : names) {
            final String[] nameCharacters = name.split("");
            for (int i = 0; i < name.length(); i++) {
                if (INVALID_NAME_AND_IDENTIFIER_CHARACTERS.contains(nameCharacters[i])) {
                    throw new DataValidationException("Invalid data.", name + "contains an invalid character");
                }
            }
        }
    }
}
