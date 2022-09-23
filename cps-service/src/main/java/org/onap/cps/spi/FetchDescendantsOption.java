/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Copyright (C) 2022 Nordix Foundation
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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FetchDescendantsOption {

    public static final FetchDescendantsOption FETCH_DIRECT_CHILDREN_ONLY = new FetchDescendantsOption(1);
    public static final FetchDescendantsOption OMIT_DESCENDANTS = new FetchDescendantsOption(0);
    public static final FetchDescendantsOption INCLUDE_ALL_DESCENDANTS = new FetchDescendantsOption(-1);

    private final int depth;

    /**
     * Has next depth.
     *
     * @return the next depth
     * @throws IllegalArgumentException when depth less than -1
     */
    public boolean hasNext() {
        if (depth < -1) {
            throw new IllegalArgumentException("A depth of less than minus one is not allowed");
        }
        return depth > 0 || this.depth == INCLUDE_ALL_DESCENDANTS.depth;
    }

    /**
     * Next fetch descendants option.
     *
     * @return the fetch descendants option
     * @throws IllegalArgumentException when depth less than -1 or 0
     */
    public FetchDescendantsOption next() {
        if (depth < -1) {
            throw new IllegalArgumentException("A depth of less than minus one is not allowed");
        }
        if (depth == 0) {
            throw new IllegalArgumentException("Do not use next() method with zero depth");
        }
        return this.depth == INCLUDE_ALL_DESCENDANTS.depth
                ? INCLUDE_ALL_DESCENDANTS : new FetchDescendantsOption(depth - 1);
    }
}
