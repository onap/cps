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

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient.AttributeValueChangeEventPublisher;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrustLevelManager {

    private final Map<String, TrustLevel> trustLevelPerCmHandle;
    private final AttributeValueChangeEventPublisher attributeValueChangeEventPublisher;
    private static final String NO_OLD_VALUE = null;
    private static final String ATTRIBUTE_TO_CHANGE = "trustLevel";


    /**
     * Add cmHandle to trustLevelPerCmHandle and publish initial trust level of cmHandle if it is NONE.
     *
     * @param cmHandlesToBeCreated
     */
    public void handleInitialRegistrationOfTrustLevels(final List<NcmpServiceCmHandle> cmHandlesToBeCreated) {
        for (final NcmpServiceCmHandle ncmpServiceCmHandle : cmHandlesToBeCreated) {
            final String cmHandleId = ncmpServiceCmHandle.getCmHandleId();
            final TrustLevel newTrustLevel = ncmpServiceCmHandle.getRegistrationTrustLevel();
            if (trustLevelPerCmHandle.containsKey(cmHandleId)) {
                log.debug("Cm handle: {} already registered", cmHandleId);
            } else {
                trustLevelPerCmHandle.put(cmHandleId, newTrustLevel);
                if (TrustLevel.NONE.equals(newTrustLevel)) {
                    sendAvcNotificationEvent(cmHandleId, NO_OLD_VALUE, newTrustLevel.name());
                }
            }
        }
    }


    /**
     * Convert a list of cmHandles to a map of cmHandles and the trust level.
     *
     * @param cmHandleId
     * @param newTrustLevel
     */
    public void handleUpdateOfTrustLevels(final String cmHandleId, final String newTrustLevel) {
        final TrustLevel oldTrustLevel = trustLevelPerCmHandle.get(cmHandleId);
        if (newTrustLevel.equals(oldTrustLevel)) {
            log.debug("The Cm Handle: {} has already the same trust level: {}", cmHandleId, newTrustLevel);
        } else {
            trustLevelPerCmHandle.put(cmHandleId, TrustLevel.valueOf(newTrustLevel));
            sendAvcNotificationEvent(cmHandleId, oldTrustLevel.name(), newTrustLevel);
            log.debug("The new trust level: {} has been updated for Cm Handle: {}", newTrustLevel, cmHandleId);
        }
    }

    private void sendAvcNotificationEvent(String cmHandleId, String oldTrustLevel, String newTrustLevel) {
        attributeValueChangeEventPublisher.publishAttributeValueChangeEvent(cmHandleId,
            ATTRIBUTE_TO_CHANGE,
            oldTrustLevel,
            newTrustLevel);
    }

}
