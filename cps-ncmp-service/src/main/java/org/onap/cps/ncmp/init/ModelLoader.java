/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.init;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.spi.model.Dataspace;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelLoader implements ApplicationListener<ApplicationReadyEvent> {

    private final CpsAdminService cpsAdminService;
    private final CpsModuleService cpsModuleService;
    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-subscription";
    private static final String SUBSCRIPTION_SCHEMASET_NAME = "subscriptions";

    /**
     * Method calls boarding subscription model when Application is ready.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(@NonNull final ApplicationReadyEvent event) {
        onboardSubscriptionModel();
    }

    /**
     * Method to onboard subscription model for NCMP.
     */
    public void onboardSubscriptionModel() {
        int onboardingRetries = 0;

        while (onboardingRetries < 10) {
            final Dataspace ncmpDataspace = cpsAdminService.getDataspace(SUBSCRIPTION_DATASPACE_NAME);
            if (ncmpDataspace == null) {
                onboardingRetries++;
                log.info("Onboarding subscription model failed, NCMP dataspace does not exist");
            } else {
                try {
                    createSchemaSetFromModelFile(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME,
                            "subscription.yang", "model/subscription.yang");
                } catch (final Exception exception) {
                    log.debug("Creating schema set for subscription model failed {}: ", exception.getMessage());
                } finally {
                    try {
                        createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME,
                                SUBSCRIPTION_ANCHOR_NAME);
                    } catch (final Exception exception) {
                        log.debug("Creating anchor for subscription model failed: {}", exception.getMessage());
                    }
                }
                break;
            }
        }
    }

    /**
     * Creates schema set from model file.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @param pathToYangModelFile path to the model file
     */
    private void createSchemaSetFromModelFile(final String dataspaceName,
                                             final String schemaSetName,
                                             final String modelFileName,
                                             final String pathToYangModelFile) {
        try {
            final Map<String, String> yangResourceContentMap =
                    createYangResourceToContentMap(modelFileName, pathToYangModelFile);
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentMap);
        } catch (final Exception exception) {
            throw new NcmpException("Onboarding failed when creating schema set: " + exception.getMessage(),
                    exception.getMessage());
        }
    }

    /**
     * Create anchor.
     *
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param schemaSetName schema set name
     */
    private void createAnchor(final String dataspaceName, final String schemaSetName,
                             final String anchorName) {
        try {
            cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName);
        } catch (final Exception exception) {
            throw new NcmpException("Onboarding failed when creating anchor: " + exception.getMessage(),
                    exception.getMessage());
        }
    }

    private String getFileContentAsString(final String pathToModel) {
        try (InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(pathToModel)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final Exception exception) {
            throw new NcmpException("Onboarding failed due to given file", exception.getCause().toString());
        }
    }

    private Map<String, String> createYangResourceToContentMap(final String fileName, final String pathToModel) {
        return Map.of(fileName, getFileContentAsString(pathToModel));
    }
}
