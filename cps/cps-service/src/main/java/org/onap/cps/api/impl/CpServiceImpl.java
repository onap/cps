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
import org.onap.cps.spi.DataPersistencyService;
import org.onap.cps.spi.ModelPersistencyService;
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
    private ModelPersistencyService modelPersistencyService;

    @Autowired
    private DataPersistencyService dataPersistencyService;

    @Override
    public final SchemaContext parseAndValidateModel(final String yangModelContent) throws IOException,
        YangParserException {
        final File tempFile = File.createTempFile("yang", ".yang");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(yangModelContent);
        }
        return parseAndValidateModel(tempFile);
    }

    @Override
    public final SchemaContext parseAndValidateModel(final File yangModelFile) throws IOException, YangParserException {
        return YangUtils.parseYangModelFile(yangModelFile);
    }

    @Override
    public final Integer storeJsonStructure(final String jsonStructure) {
        return dataPersistencyService.storeJsonStructure(jsonStructure);
    }

    @Override
    public final String getJsonById(final int jsonObjectId) {
        return dataPersistencyService.getJsonById(jsonObjectId);
    }

    @Override
    public void deleteJsonById(int jsonObjectId) {
        dataPersistencyService.deleteJsonById(jsonObjectId);
    }

    @Override
    public final void storeSchemaContext(final SchemaContext schemaContext, final String dataspaceName) {
        for (final Module module : schemaContext.getModules()) {
            Optional<Revision> optionalRevision = module.getRevision();
            String revisionValue = optionalRevision.isPresent() ? optionalRevision.get().toString() : null;
            modelPersistencyService.storeModule(module.getNamespace().toString(), module.toString(),
                revisionValue, dataspaceName);
        }
    }
}
