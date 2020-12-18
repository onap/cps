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


import java.util.Map;
import java.util.Optional;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.utils.YangUtils;
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("CpsModuleServiceImpl")
public class CpsModuleServiceImpl implements CpsModuleService {

    @Autowired
    private CpsModulePersistenceService cpsModulePersistenceService;

    @Override
    public void createSchemaSet(final String dataspaceName, final String schemaSetName,
                                final Map<String, String> yangResourcesNameToContentMap) {

        YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap).getSchemaContext(); // validation
        cpsModulePersistenceService
            .storeSchemaSet(dataspaceName, schemaSetName, yangResourcesNameToContentMap);
    }

}
