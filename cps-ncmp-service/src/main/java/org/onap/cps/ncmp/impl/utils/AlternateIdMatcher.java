/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.utils.CpsValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlternateIdMatcher {

    private final InventoryPersistence inventoryPersistence;
    private final CpsValidator cpsValidator;

    /**
     * Get cm handle that matches longest alternate id by removing elements
     * (as defined by the separator string) from right to left.
     * If alternate id contains a hash then all elements after that hash are ignored.
     *
     * @param alternateId            alternate ID
     * @param separator              a string that separates each element from the next.
     * @param cmHandlePerAlternateId all CM-handles by alternate ID
     * @return ncmp service cm handle
     */
    public NcmpServiceCmHandle getCmHandleByLongestMatchingAlternateId(
            final String alternateId, final String separator,
            final Map<String, NcmpServiceCmHandle> cmHandlePerAlternateId) {
        final String[] splitPath = alternateId.split("#", 2);
        String bestMatch = splitPath[0];
        while (StringUtils.isNotEmpty(bestMatch)) {
            final NcmpServiceCmHandle ncmpServiceCmHandle = cmHandlePerAlternateId.get(bestMatch);
            if (ncmpServiceCmHandle != null) {
                return ncmpServiceCmHandle;
            }
            bestMatch = getParentPath(bestMatch, separator);
        }
        throw new NoAlternateIdMatchFoundException(alternateId);
    }

    /**
     * Get cm handle Id from given cmHandleReference.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle id string
     */
    public String getCmHandleId(final String cmHandleReference) {
        if (cpsValidator.isValidName(cmHandleReference)) {
            return getCmHandleIdTryingStandardIdFirst(cmHandleReference);
        }
        return getCmHandleIdByAlternateId(cmHandleReference);
    }

    private String getCmHandleIdByAlternateId(final String cmHandleReference) {
        // Please note: because of cm handle id validation rules this case does NOT need to try by (standard) id
        return inventoryPersistence.getYangModelCmHandleByAlternateId(cmHandleReference).getId();
    }

    private String getCmHandleIdTryingStandardIdFirst(final String cmHandleReference) {
        if (inventoryPersistence.isExistingCmHandleId(cmHandleReference)) {
            return cmHandleReference;
        }
        return inventoryPersistence.getYangModelCmHandleByAlternateId(cmHandleReference).getId();
    }

    private String getParentPath(final String path, final String separator) {
        final int lastSeparatorIndex = path.lastIndexOf(separator);
        return lastSeparatorIndex < 0 ? "" : path.substring(0, lastSeparatorIndex);
    }
}
