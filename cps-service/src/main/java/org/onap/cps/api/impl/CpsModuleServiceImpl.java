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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("CpsModuleServiceImpl")
public class CpsModuleServiceImpl implements CpsModuleService {

    @Autowired
    private CpsModulePersistenceService cpsModulePersistenceService;

    @Override
    public SchemaContext parseAndValidateModel(final Map<String, String> yangModelMap) {
        try {
            return YangUtils.generateSchemaContext(yangModelMap);
        } catch (final YangParserException | ReactorException e) {
            throw new ModelValidationException("Yang file validation failed", e.getMessage(), e);
        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }

    @Override
    public SchemaContext parseAndValidateModel(final List<File> yangModelFile) {
        try {
            return YangUtils.parseYangModelFiles(yangModelFile);
        } catch (final YangParserException | ReactorException e) {
            throw new ModelValidationException("Yang file validation failed", e.getMessage(), e);
        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }

    @Override
    public void storeSchemaContext(final SchemaContext schemaContext, final String dataspaceName) {
        for (final Module module : schemaContext.getModules()) {
            final Optional<Revision> optionalRevision = module.getRevision();
            final String revisionValue = optionalRevision.map(Object::toString).orElse(null);
            cpsModulePersistenceService.storeModule(module.getNamespace().toString(), module.toString(),
                revisionValue, dataspaceName);
        }
    }
}
