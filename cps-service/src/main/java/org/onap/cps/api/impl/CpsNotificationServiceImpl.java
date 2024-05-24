/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.api.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsNotificationService;
import org.onap.cps.spi.CpsNotificationPersistenceService;
import org.onap.cps.spi.utils.CpsValidator;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsNotificationServiceImpl implements CpsNotificationService {

    private final CpsValidator cpsValidator;

    private final CpsNotificationPersistenceService cpsNotificationPersistenceService;

    @Override
    public void updateNotificationSubscription(final String dataspaceName, final String subscription,
                                               final List<String> anchors) {

        cpsValidator.validateNameCharacters(dataspaceName);

        if (anchors == null || anchors.isEmpty()) {
            if ("subscribe".equals(subscription)) {
                cpsNotificationPersistenceService.subscribeNotificationForAllAnchors(dataspaceName);
            } else {
                cpsNotificationPersistenceService.unsubscribeNotificationForAllAnchors(dataspaceName);
            }
        } else {
            for (final String anchorName : anchors) {
                if ("subscribe".equals(subscription)) {
                    cpsNotificationPersistenceService.subscribeNotification(dataspaceName, anchorName);
                } else {
                    cpsNotificationPersistenceService.unsubscribeNotification(dataspaceName, anchorName);
                }
            }
        }
    }

    @Override
    public boolean isNotificationEnabled(final String dataspaceName, final String anchorName) {
        return cpsNotificationPersistenceService.isNotificationSubscribed(dataspaceName, anchorName);
    }
}
