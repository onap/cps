/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class FetchDescendantsOption {

    public static final FetchDescendantsOption FETCH_DIRECT_CHILDREN_ONLY = new FetchDescendantsOption(1);
    public static final FetchDescendantsOption OMIT_DESCENDANTS = new FetchDescendantsOption(0);
    public static final FetchDescendantsOption INCLUDE_ALL_DESCENDANTS = new FetchDescendantsOption(-1);

    private static final String DESCENDANT_PATTERN = "^all$|^none$|^[0-9]+$|^-1$";

    private final int depth;

    /**
     * Has next depth.
     *
     * @return true if next level of depth is available
     * @throws IllegalArgumentException when depth less than -1
     */
    public boolean hasNext() {
        validateDepth(depth);
        return depth > 0 || this.depth == INCLUDE_ALL_DESCENDANTS.depth;
    }

    /**
     * Next fetch descendants option.
     *
     * @return the next fetch descendants option
     * @throws IllegalArgumentException when depth less than -1 or 0
     */
    public FetchDescendantsOption next() {
        if (depth == 0) {
            throw new IllegalArgumentException("Do not use next() method with zero depth");
        }
        final FetchDescendantsOption nextDescendantsOption = this.depth == INCLUDE_ALL_DESCENDANTS.depth
                ? INCLUDE_ALL_DESCENDANTS : new FetchDescendantsOption(depth - 1);
        validateDepth(nextDescendantsOption.depth);
        return nextDescendantsOption;
    }

    /**
     * get fetch descendants option for given descendant.
     *
     * @param descendants descendants
     * @return fetch descendants option for given descendant
     */
    public static FetchDescendantsOption getFetchDescendantOption(final String descendants) {
        validateDescendant(descendants);
        if (null == descendants || descendants.trim().isEmpty()
                || "0".equals(descendants) || "none".equals(descendants)) {
            return FetchDescendantsOption.OMIT_DESCENDANTS;
        } else if ("-1".equals(descendants) || "all".equals(descendants)) {
            return FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
        } else {
            final Integer depth = Integer.valueOf(descendants);
            return new FetchDescendantsOption(depth);
        }
    }

    private static void validateDescendant(final String descendant) {
        if (!StringUtils.hasLength(descendant)) {
            return;
        }
        final Pattern pattern = Pattern.compile(DESCENDANT_PATTERN);
        final Matcher matcher = pattern.matcher(descendant);
        if (!matcher.matches()) {
            throw new DataValidationException("Descendant validation error.",
                    descendant + " is not valid descendant");
        }
    }

    private static void validateDepth(final int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException("A depth of less than minus one is not allowed");
        }
    }

}
