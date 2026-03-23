/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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

package org.onap.cps.ri.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Concrete DTO for delta query results. Eliminates JDK dynamic proxy overhead
 * of interface-based projections by storing values in plain fields.
 */
@Getter
@AllArgsConstructor
public class DeltaProjectionDto {

    private final String xpath;
    private final Long sourceId;
    private final Long targetId;
    private final String sourceAttributes;
    private final String targetAttributes;

    /**
     * Convert a proxy-based DeltaProjection into a plain DTO.
     *
     * @param projection the interface projection proxy
     * @return a concrete DTO with the same data
     */
    public static DeltaProjectionDto from(final DeltaProjection projection) {
        return new DeltaProjectionDto(
            projection.getXpath(),
            projection.getSourceId(),
            projection.getTargetId(),
            projection.getSourceAttributes(),
            projection.getTargetAttributes()
        );
    }
}

