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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ModelLoader implements CommandLineRunner {

    private final String cpsCoreHost = System.getenv("CPS_CORE_HOST");
    private final String cpsUsername = System.getenv("CPS_USERNAME");
    private final String cpsPassword = System.getenv("CPS_PASSWORD");
    private static final String HTTP = "HTTP://";

    @Override
    public void run(final String... args) {

        final boolean ncmpDataspaceExists = getAllDataspaces().contains("NCMP-Admin");

        if (ncmpDataspaceExists) {
            try {
                log.info("Check that dataspace NCMP-Admin exists: {}", ncmpDataspaceExists);
                createSchemaSetFromYangFile("NCMP-Admin", "subscriptions", "model/subscription.yang");
                createAnchor("AVC-subscriptions", "subscriptions");
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        } else {
            run("");
        }
    }

    /**
     * Get all dataspaces via CPS REST API.
     *
     * @return String response body
     */
    public String getAllDataspaces() {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(cpsUsername, cpsPassword);

        final HttpEntity<String> request = new HttpEntity<>(headers);

        final String url = HTTP + cpsCoreHost + ":8883/cps/api/v2/admin/dataspaces";
        final ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        log.info("Response for getting all dataspaces via NCMP: {}", response.getBody());
        if (response.getBody() == null) {
            return "";
        }
        return response.getBody();
    }

    /**
     * Creates schema set via CPS REST API.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param pathToYangModelFile path to the model file
     */
    public void createSchemaSetFromYangFile(final String dataspaceName,
                                            final String schemaSetName,
                                            final String pathToYangModelFile) {
        final MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        final File file = getModelAsFile(pathToYangModelFile);

        parts.add("file", new FileSystemResource(file));

        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        final String url =
                HTTP + cpsCoreHost + ":8883/cps/api/v2/dataspaces/{param1}/schema-sets?schema-set-name={param2}";
        headers.setBasicAuth(cpsUsername, cpsPassword);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        final HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
        final ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class, dataspaceName, schemaSetName);

        log.info("Response for creating schema set in {} : {} ", dataspaceName, response.getBody());
    }

    /**
     * Create anchor via CPS REST API.
     *
     * @param anchorName anchor name
     * @param schemaSetName schema set name
     */
    public void createAnchor(final String anchorName, final String schemaSetName) {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        final HttpEntity<String> request = new HttpEntity<>(headers);
        final String url =
                HTTP + cpsCoreHost
                        + ":8883/cps/api/v2/dataspaces/NCMP-Admin/anchors?"
                        + "schema-set-name={param1}&anchor-name={param2}";

        headers.setBasicAuth(cpsUsername, cpsPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);
        final ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.POST, request, String.class, schemaSetName, anchorName);

        log.info("Response for creating anchor: {}", response.getBody());
    }

    private File getModelAsFile(final String pathToModel) {

        try (InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(pathToModel)) {
            assert inputStream != null;

            final File modelFile = File.createTempFile("subscription", ".yang");
            final File tmpFile = new File(modelFile.getParent(), "subscription.yang");

            final boolean modelFileRenamed = modelFile.renameTo(tmpFile);

            if (modelFileRenamed) {
                try (final FileOutputStream outputStream = new FileOutputStream(modelFile)) {
                    final byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                }

                modelFile.deleteOnExit();
                tmpFile.deleteOnExit();
            }
            return modelFile;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
