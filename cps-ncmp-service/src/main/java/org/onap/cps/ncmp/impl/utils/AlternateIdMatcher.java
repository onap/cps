/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlternateIdMatcher {

    private final InventoryPersistence inventoryPersistence;

    /**
     * Get data node that matches longest alternate id by removing elements (as defined by the separator string)
     * from right to left.
     *
     * @param alternateId alternate ID
     * @param separator   a string that separates each element from the next.
     * @return data node
     */
    public DataNode getCmHandleDataNodeByLongestMatchingAlternateId(final String alternateId, final String separator) {
        String bestMatch = alternateId;
        while (StringUtils.isNotEmpty(bestMatch)) {
            try {
                return inventoryPersistence.getCmHandleDataNodeByAlternateId(bestMatch);
            } catch (final DataNodeNotFoundException ignored) {
                bestMatch = getParentPath(bestMatch, separator);
            }
        }
        throw new NoAlternateIdMatchFoundException(alternateId);
    }

    /**
     * Check database if cm handle id exists if not return false.
     *
     * @param cmHandle cmHandle Id
     * @return Boolean
     */
    public Boolean isExistingCmHandleId(final String cmHandle) {
        try {
            inventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandle).iterator().next();
            return true;
        } catch (final DataNodeNotFoundException exception) {
            return false;
        }
    }

    /**
     * Get cm handle Id from given Alternate Id.
     *
     * @param alternateId alternate ID
     * @return cm handle id string
     */
    public String getCmHandleIdFromAlternateId(final String alternateId) {
        final DataNode dataNode = inventoryPersistence.getCmHandleDataNodeByAlternateId(alternateId);
        return dataNode.getLeaves().get("id").toString();
    }

    private String getParentPath(final String path, final String separator) {
        final int lastSeparatorIndex = path.lastIndexOf(separator);
        return lastSeparatorIndex < 0 ? "" : path.substring(0, lastSeparatorIndex);
    }
}
