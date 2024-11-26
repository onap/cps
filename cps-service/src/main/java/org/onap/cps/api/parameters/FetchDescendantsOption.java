/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.api.parameters;

import com.google.common.base.Strings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;

@RequiredArgsConstructor
public class FetchDescendantsOption {

    public static final FetchDescendantsOption DIRECT_CHILDREN_ONLY
        = new FetchDescendantsOption(1, "DirectChildrenOnly");
    public static final FetchDescendantsOption OMIT_DESCENDANTS
        = new FetchDescendantsOption(0, "OmitDescendants");
    public static final FetchDescendantsOption INCLUDE_ALL_DESCENDANTS
        = new FetchDescendantsOption(-1, "IncludeAllDescendants");

    FetchDescendantsOption(final int depth) {
        this(depth, "Depth=" + depth);
    }

    private static final Pattern FETCH_DESCENDANTS_OPTION_PATTERN =
        Pattern.compile("^$|^all$|^none$|^direct$|^[0-9]+$|^-1$|^1$");

    private final int depth;

    private final String optionName;

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
     * Get depth.
     * @return depth: -1 for all descendants, 0 for no descendants, or positive value for fixed level of descendants
     */
    public int getDepth() {
        return depth;
    }

    /**
     * get fetch descendants option for given descendant.
     *
     * @param fetchDescendantsOptionAsString fetch descendants option string
     * @return fetch descendants option for given descendant
     */
    public static FetchDescendantsOption getFetchDescendantsOption(final String fetchDescendantsOptionAsString) {
        validateFetchDescendantsOption(fetchDescendantsOptionAsString);
        if (Strings.isNullOrEmpty(fetchDescendantsOptionAsString)
                || "0".equals(fetchDescendantsOptionAsString) || "none".equals(fetchDescendantsOptionAsString)) {
            return FetchDescendantsOption.OMIT_DESCENDANTS;
        } else if ("-1".equals(fetchDescendantsOptionAsString) || "all".equals(fetchDescendantsOptionAsString)) {
            return FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
        } else if ("1".equals(fetchDescendantsOptionAsString) || "direct".equals(fetchDescendantsOptionAsString)) {
            return FetchDescendantsOption.DIRECT_CHILDREN_ONLY;
        } else {
            final Integer depth = Integer.valueOf(fetchDescendantsOptionAsString);
            return new FetchDescendantsOption(depth);
        }
    }

    @Override
    public String toString() {
        return optionName;
    }

    private static void validateFetchDescendantsOption(final String fetchDescendantsOptionAsString) {
        if (Strings.isNullOrEmpty(fetchDescendantsOptionAsString)) {
            return;
        }
        final Matcher matcher = FETCH_DESCENDANTS_OPTION_PATTERN.matcher(fetchDescendantsOptionAsString);
        if (!matcher.matches()) {
            throw new DataValidationException("FetchDescendantsOption validation error.",
                    fetchDescendantsOptionAsString + " is not valid fetch descendants option");
        }
    }

    private static void validateDepth(final int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException("A depth of less than minus one is not allowed");
        }
    }

}
