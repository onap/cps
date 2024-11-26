/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ri.utils;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.impl.utils.CpsValidator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CpsValidatorImpl implements CpsValidator {

    private static final char[] UNSUPPORTED_NAME_CHARACTERS = "!\" #$%&'()*+,./\\:;<=>?@[]^`{|}~".toCharArray();

    @Override
    public void validateNameCharacters(final String... names) {
        validateNameCharacters(Arrays.asList(names));
    }

    @Override
    public void validateNameCharacters(final Iterable<String> names) {
        for (final String name : names) {
            final Collection<Character> charactersOfName = Lists.charactersOf(name);
            for (final char unsupportedCharacter : UNSUPPORTED_NAME_CHARACTERS) {
                if (charactersOfName.contains(unsupportedCharacter)) {
                    throw new DataValidationException("Name or ID Validation Error.",
                        name + " invalid token encountered at position "
                            + (name.indexOf(unsupportedCharacter) + 1));
                }
            }
        }
    }

    @Override
    public void validatePaginationOption(final PaginationOption paginationOption) {
        if (PaginationOption.NO_PAGINATION == paginationOption) {
            return;
        }

        if (!paginationOption.isValidPaginationOption()) {
            throw new DataValidationException("Pagination validation error.",
                    "Invalid page index or size");
        }
    }
}
