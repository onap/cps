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

package org.onap.cps.spi.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.CpsNotificationPersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsNotificationPersistenceServiceImpl implements CpsNotificationPersistenceService {

    private final static String SUBSCRIPTION_XPATH = "/notification-subscription";

    private final DataspaceRepository dataspaceRepository;

    private final AnchorRepository anchorRepository;

    private final FragmentRepository fragmentRepository;

    @Override
    public void subscribeNotification(String dataspaceName, String anchorName) {
        AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        enableNotificationForAnchor(anchorEntity);
    }

    private void enableNotificationForAnchor(AnchorEntity anchorEntity){
        FragmentEntity fragmentEntity = new FragmentEntity();
        fragmentEntity.setXpath(SUBSCRIPTION_XPATH);
        fragmentEntity.setAnchor(anchorEntity);
        try {
            fragmentRepository.save(fragmentEntity);
        } catch (Exception ex) {
            log.warn("Notification is already enabled for anchor {}", anchorEntity.getName());
        }
    }

    private AnchorEntity getAnchorEntity(final String dataspaceName, final String anchorName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
    }

    @Override
    @Transactional
    public void unsubscribeNotification(String dataspaceName, String anchorName) {
        AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        disableNotificationForAnchor(anchorEntity);
    }

    private void disableNotificationForAnchor(AnchorEntity anchorEntity) {
        List<String> xpaths = new ArrayList<>(1);
        xpaths.add(SUBSCRIPTION_XPATH);
        fragmentRepository.deleteByAnchorIdAndXpaths(anchorEntity.getId(), xpaths);
    }

    @Override
    public void subscribeNotificationForAllAnchors(String dataspaceName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        Collection<AnchorEntity> anchorEntities = anchorRepository.findAllByDataspace(dataspaceEntity);
        for(AnchorEntity anchorEntity : anchorEntities) {
            enableNotificationForAnchor(anchorEntity);
        }
    }

    @Override
    @Transactional
    public void unsubscribeNotificationForAllAnchors(String dataspaceName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        Collection<AnchorEntity> anchorEntities = anchorRepository.findAllByDataspace(dataspaceEntity);
        for(AnchorEntity anchorEntity : anchorEntities) {
            disableNotificationForAnchor(anchorEntity);
        }
    }

    @Override
    public boolean isNotificationSubscribed(String dataspaceName, String anchorName) {
        AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        return fragmentRepository.findByAnchorAndXpath(anchorEntity, SUBSCRIPTION_XPATH).isPresent();
    }
}
