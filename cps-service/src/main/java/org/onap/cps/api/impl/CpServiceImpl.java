/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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
import java.util.Optional;
import org.onap.cps.api.CpService;
import org.onap.cps.api.model.AnchorDetails;
import org.onap.cps.exceptions.CpsException;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.DataPersistenceService;
import org.onap.cps.spi.FragmentPersistenceService;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpServiceImpl implements CpService {

    @Autowired
    private ModelPersistenceService modelPersistenceService;

    @Autowired
    private DataPersistenceService dataPersistenceService;

    @Autowired
    private FragmentPersistenceService fragmentPersistenceService;

    @Override
    public final SchemaContext parseAndValidateModel(final String yangModelContent) {

        try {
            final File tempFile = File.createTempFile("yang", ".yang");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(yangModelContent);
            }
            return parseAndValidateModel(tempFile);
        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }

    @Override
    public final SchemaContext parseAndValidateModel(final File yangModelFile) {
        try {
            return YangUtils.parseYangModelFile(yangModelFile);
        } catch (final YangParserException e) {
            throw new CpsValidationException("Yang file validation failed", e.getMessage());
        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }

    @Override
    public final Integer storeJsonStructure(final String jsonStructure) {
        return dataPersistenceService.storeJsonStructure(jsonStructure);
    }

    @Override
    public final String getJsonById(final int jsonObjectId) {
        return dataPersistenceService.getJsonById(jsonObjectId);
    }

    @Override
    public void deleteJsonById(final int jsonObjectId) {
        dataPersistenceService.deleteJsonById(jsonObjectId);
    }

    @Override
    public final void storeSchemaContext(final SchemaContext schemaContext, final String dataspaceName) {
        for (final Module module : schemaContext.getModules()) {
            final Optional<Revision> optionalRevision = module.getRevision();
            final String revisionValue = optionalRevision.map(Object::toString).orElse(null);
            modelPersistenceService.storeModule(module.getNamespace().toString(), module.toString(),
                revisionValue, dataspaceName);
        }
    }

    @Override
    public String createAnchor(final AnchorDetails anchorDetails) {
        return fragmentPersistenceService.createAnchor(anchorDetails);
    }
}