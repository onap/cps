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

package org.onap.cps.ncmp.api.models;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.model.Dataspace;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelLoader implements CommandLineRunner {

    private final CpsAdminService cpsAdminService;
    private final CpsModuleService cpsModuleService;

    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-subscription";

    @Override
    public void run(final String... args) {
        onboardSubscriptionModel();
    }

    /**
     * Method to onboard subscription model for NCMP.
     */
    public void onboardSubscriptionModel() {
        final Dataspace ncmpDataspace = cpsAdminService.getDataspace(SUBSCRIPTION_DATASPACE_NAME);

        if (ncmpDataspace != null) {
            try {
                log.info("Check that dataspace NCMP-Admin exists: {}", ncmpDataspace);
                createSchemaSetFromModelFile(SUBSCRIPTION_DATASPACE_NAME, "subscriptions",
                        "subscription.yang", "model/subscription.yang");
                createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME, "subscriptions");
            } catch (final Exception exception) {
                log.debug("Onboard model exception: {}", exception.getMessage());
            }
        } else {
            onboardSubscriptionModel();
        }
    }

    /**
     * Creates schema set from model file.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param pathToYangModelFile path to the model file
     */
    public void createSchemaSetFromModelFile(final String dataspaceName,
                                             final String schemaSetName,
                                             final String modelFileName,
                                             final String pathToYangModelFile) {
        final MultipartFile multipartFile = createMultiPartFileForModel(modelFileName,
                pathToYangModelFile);
        final Map<String, String> yangResourceContentMap = createYangResourceToContentMap(multipartFile);
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentMap);
    }

    /**
     * Create anchor.
     *
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param schemaSetName schema set name
     */
    public void createAnchor(final String dataspaceName, final String anchorName,
                             final String schemaSetName) {
        cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName);
    }

    private MultipartFile createMultiPartFileForModel(final String fileName, final String pathToModel) {
        return new MockMultipartFile(fileName, getModelFileAsByteArray(pathToModel));
    }

    private byte[] getModelFileAsByteArray(final String pathToModel) {
        try (InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(pathToModel)) {
            assert inputStream != null;
            return new byte[inputStream.available()];
        } catch (final IOException exception) {
            log.debug("Onboard model exception [getModelFileAsByteArray]: {}",
                    exception.getMessage());
        }
        return new byte[0];
    }

    private Map<String, String> createYangResourceToContentMap(final MultipartFile multipartFile) {
        final String modelFileName = multipartFile.getOriginalFilename();
        try {
            final String content = new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
            assert modelFileName != null;
            return Map.of(modelFileName, content);
        } catch (final IOException exception) {
            log.debug("Onboard model exception [createYangResourceToContent]: {}",
                    exception.getMessage());
        }
        return Collections.emptyMap();
    }
}
