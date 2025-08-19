/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.utils;

import com.hazelcast.map.IMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.exceptions.CmHandleNotFoundException;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlternateIdMatcher {

    @Qualifier("cmHandleIdPerAlternateId")
    private final IMap<String, String> cmHandleIdPerAlternateId;

    private static final String URI_PATH_COMPONENT_SEPARATOR = "#";

    /**
     * Get cm handle that matches longest alternate id by removing elements
     * (as defined by the separator string) from right to left.
     * If alternate id contains a hash then all elements after that hash are ignored.
     *
     * @param alternateId            alternate ID
     * @param separator              a string that separates each path element from the next.
     * @return ncmp service cm handle
     */
    public String getCmHandleIdByLongestMatchingAlternateId(final String alternateId, final String separator) {
        final String[] uriPathComponents = alternateId.split(URI_PATH_COMPONENT_SEPARATOR, 2);
        String bestMatch = uriPathComponents[0];
        while (StringUtils.isNotEmpty(bestMatch)) {
            final String cmHandleId = cmHandleIdPerAlternateId.get(bestMatch);
            if (cmHandleId != null) {
                return cmHandleId;
            }
            bestMatch = getParentPath(bestMatch, separator);
        }
        throw new NoAlternateIdMatchFoundException(alternateId);
    }

    /**
     * Get cm handle that matches longest alternate id in the given map.
     *
     * @param alternateId            the target alternate id
     * @param separator              a string that separates each path element from the next.
     * @param cmHandlePerAlternateId a map of cm handles with the alternate id as key
     * @return cm handle as a YangModelCmHandle
     */
    public YangModelCmHandle getCmHandleByLongestMatchingAlternateId(
                                                    final String alternateId,
                                                    final String separator,
                                                    final Map<String, YangModelCmHandle> cmHandlePerAlternateId) {
        final String[] splitPathOnHashExtension = alternateId.split("#", 2);
        String bestMatch = splitPathOnHashExtension[0];
        while (StringUtils.isNotEmpty(bestMatch)) {
            final YangModelCmHandle yangModelCmHandle = cmHandlePerAlternateId.get(bestMatch);
            if (yangModelCmHandle != null) {
                return yangModelCmHandle;
            }
            bestMatch = getParentPath(bestMatch, separator);
        }
        throw new NoAlternateIdMatchFoundException(alternateId);
    }

    /**
     * Get collection of cm handle ids whose alternate id best (longest) match the given paths.
     * If alternate id contains a hash then all elements after that hash are ignored.
     *
     * @param paths            collection of paths
     * @param separator        a string that separates each path element from the next.
     * @return collection of cm handle ids
     */
    public Collection<String> getCmHandleIdsByLongestMatchingAlternateIds(final Collection<String> paths,
                                                                          final String separator) {
        final Collection<String> cmHandleIds = new ArrayList<>();
        Set<String> unresolvedPaths = new HashSet<>(paths);
        while (!unresolvedPaths.isEmpty()) {
            final Map<String, String> resolvedCmHandleIdPerAlternateId
                = cmHandleIdPerAlternateId.getAll(unresolvedPaths);
            cmHandleIds.addAll(resolvedCmHandleIdPerAlternateId.values());
            unresolvedPaths.removeAll(resolvedCmHandleIdPerAlternateId.keySet());
            unresolvedPaths = unresolvedPaths.stream().map(p -> getParentPath(p, separator))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        }
        return cmHandleIds;
    }

    /**
     * Get cm handle id from given cmHandleReference.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle id string
     */
    public String getCmHandleId(final String cmHandleReference) {
        final String cmHandleId = cmHandleIdPerAlternateId.get(cmHandleReference);
        if (cmHandleId == null) {
            if (cmHandleIdPerAlternateId.containsValue(cmHandleReference)) {
                return cmHandleReference;
            } else {
                throw new CmHandleNotFoundException(cmHandleReference);
            }
        }
        return cmHandleId;
    }

    private String getParentPath(final String path, final String separator) {
        final int lastSeparatorIndex = path.lastIndexOf(separator);
        return lastSeparatorIndex < 0 ? "" : path.substring(0, lastSeparatorIndex);
    }
}
