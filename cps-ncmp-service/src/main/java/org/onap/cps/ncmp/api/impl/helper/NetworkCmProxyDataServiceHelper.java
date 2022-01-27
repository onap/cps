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

package org.onap.cps.ncmp.api.impl.helper;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.DataNode;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NetworkCmProxyDataServiceHelper {

    /**
     * Helper method to handle the add/remove/update of the cm-handle during cm-handle registration.
     *
     * @param dataNode       data-node
     * @param attributeKey   dmi or public cm-handle property key
     * @param attributeValue dmi or public cm-handle property value
     */
    public static void handleAddOrRemoveCmHandleProperties(final DataNode dataNode, final String attributeKey,
            final String attributeValue) {

        final Map<String, Object> leaves = dataNode.getLeaves();
        if (leaves.containsKey(attributeKey)) {
            if (attributeValue == null) {
                log.info("Removing the attribute with ( key : {} , existingValue : {} )", attributeKey,
                        leaves.get(attributeKey));
                leaves.remove(attributeKey);
            } else {
                log.info("Updating the attribute with ( key : {} , existingValue : {} to newValue : {} )", attributeKey,
                        leaves.get(attributeKey), attributeValue);
                leaves.put(attributeKey, attributeValue);
            }

        } else if (attributeValue != null) {
            log.info("Adding the attribute with ( key : {} , value : {} )", attributeKey, attributeValue);
            leaves.put(attributeKey, attributeValue);
        } else {
            log.info("Ignoring the attribute with ( key : {} ) as its value is null", attributeKey);
        }
        
    }

}
