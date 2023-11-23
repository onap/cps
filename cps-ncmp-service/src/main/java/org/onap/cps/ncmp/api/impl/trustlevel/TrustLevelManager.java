/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient.AttributeValueChangeEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustLevelManager {

    private final Map<String, TrustLevel> trustLevelPerCmHandle;
    private final AttributeValueChangeEventPublisher attributeValueChangeEventPublisher;
    private static final String NO_OLD_VALUE = null;
    private static final String CHANGED_ATTRIBUTE_NAME = "trustLevel";


    /**
     * Add cmHandles to the cache and publish notification for initial trust level of cmHandles if it is NONE.
     *
     * @param cmHandlesToBeCreated a list of cmHandles being created
     */
    public void handleInitialRegistrationOfTrustLevels(final Map<String, TrustLevel> cmHandlesToBeCreated) {
        for (final Map.Entry<String, TrustLevel> cmHandleTrustLevel : cmHandlesToBeCreated.entrySet()) {
            final String cmHandleId = cmHandleTrustLevel.getKey();
            if (trustLevelPerCmHandle.containsKey(cmHandleId)) {
                log.warn("Cm handle: {} already registered", cmHandleId);
            } else {
                TrustLevel initialTrustLevel = cmHandleTrustLevel.getValue();
                if (initialTrustLevel == null) {
                    initialTrustLevel = TrustLevel.COMPLETE;
                }
                trustLevelPerCmHandle.put(cmHandleId, initialTrustLevel);
                if (TrustLevel.NONE.equals(initialTrustLevel)) {
                    sendAvcNotificationEvent(cmHandleId, NO_OLD_VALUE, initialTrustLevel.name());
                }
            }
        }
    }


    /**
     * Update a cmHandle in the cache and publish notification if the trust level is different.
     *
     * @param cmHandleId    id of the cmHandle being updated
     * @param newTrustLevel new trust level of the cmHandle being updated
     */
    public void handleUpdateOfTrustLevels(final String cmHandleId, final String newTrustLevel) {
        final TrustLevel oldTrustLevel = trustLevelPerCmHandle.get(cmHandleId);
        if (newTrustLevel.equals(oldTrustLevel.name())) {
            log.debug("The Cm Handle: {} has already the same trust level: {}", cmHandleId, newTrustLevel);
        } else {
            trustLevelPerCmHandle.put(cmHandleId, TrustLevel.valueOf(newTrustLevel));
            sendAvcNotificationEvent(cmHandleId, oldTrustLevel.name(), newTrustLevel);
            log.info("The new trust level: {} has been updated for Cm Handle: {}", newTrustLevel, cmHandleId);
        }
    }

    private void sendAvcNotificationEvent(final String cmHandleId,
                                          final String oldTrustLevel,
                                          final String newTrustLevel) {
        attributeValueChangeEventPublisher.publishAttributeValueChangeEvent(cmHandleId,
                CHANGED_ATTRIBUTE_NAME,
                oldTrustLevel,
                newTrustLevel);
    }

}
