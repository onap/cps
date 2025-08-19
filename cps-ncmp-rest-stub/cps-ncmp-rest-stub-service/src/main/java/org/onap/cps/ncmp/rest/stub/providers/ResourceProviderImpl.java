/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.stub.providers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ResourceProviderImpl implements ResourceProvider {

    private final String pathToResponseFiles;

    @Autowired
    public ResourceProviderImpl(@Value("${stub.path}") final String pathToResponseFiles) {
        this.pathToResponseFiles = pathToResponseFiles;
    }

    @Override
    public Optional<InputStream> getResourceInputStream(final String filename) throws IOException {
        final Path path = Paths.get(pathToResponseFiles).resolve(filename);

        if (Files.exists(path)) {
            log.info("Found resource file on file system using path: {}", path);
            return Optional.of(Files.newInputStream(path));
        }

        log.warn("Couldn't find file on file system '{}', will search it in classpath", path);

        final ClassPathResource resource = new ClassPathResource(path.toString());
        if (resource.exists()) {
            log.info("Found resource in classpath using path: {}", path);
            return Optional.of(resource.getInputStream());
        }

        log.error("{} file not found on classpath or on file system", path);
        return Optional.empty();
    }

}
