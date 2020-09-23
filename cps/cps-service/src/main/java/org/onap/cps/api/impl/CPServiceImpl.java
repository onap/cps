/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.onap.cps.api.CPService;
import org.onap.cps.spi.ModelPersistencyService;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.model.repo.api.StatementParserMode;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CPServiceImpl implements CPService {

    private final static Logger LOGGER = LoggerFactory.getLogger(CPServiceImpl.class);

    private static final YangParserFactory PARSER_FACTORY;

    static {
        final Iterator<YangParserFactory> it =
            ServiceLoader.load(YangParserFactory.class).iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("No YangParserFactory found");
        }
        PARSER_FACTORY = it.next();
    }

    @Autowired
    private ModelPersistencyService modelPersistencyService;

    @Override
    public SchemaContext parseAndValidateModel(final String yangModelContent)
        throws IOException, YangParserException {
        final File tempFile = File.createTempFile("yang", ".yang");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(yangModelContent);
        } catch (IOException e) {
            LOGGER.error("Unable to write to temporary file {}", e.getMessage());
        }
        return parseAndValidateModel(tempFile);
    }

    @Override
    public SchemaContext parseAndValidateModel(final File yangModelFile) throws IOException, YangParserException {
        final YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.forFile(yangModelFile);
        final YangParser yangParser = PARSER_FACTORY.createParser(StatementParserMode.DEFAULT_MODE);
        yangParser.addSource(yangTextSchemaSource);
        return yangParser.buildEffectiveModel();
    }

    @Override
    public void storeSchemaContext(final SchemaContext schemaContext) {
        for (final Module module : schemaContext.getModules()) {
            modelPersistencyService.storeModule(module.getName(), module.toString(),
                module.getRevision().toString());
        }
    }
}
