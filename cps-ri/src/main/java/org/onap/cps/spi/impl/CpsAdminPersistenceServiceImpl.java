/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
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

package org.onap.cps.spi.impl;

import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.entities.SchemaSet;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CpsAdminPersistenceServiceImpl implements CpsAdminPersistenceService {

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {

        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final SchemaSet schemaSet = schemaSetRepository.findByDataspaceAndName(dataspace, schemaSetName)
            .orElseThrow(
                () -> new CpsValidationException("Schema set does not exist.",
                    String.format("Schema set with name %s was not found for dataspace %s.", schemaSetName,
                        dataspaceName))
            );

        final Fragment anchor = Fragment.builder()
            .xpath(anchorName)
            .anchorName(anchorName)
            .dataspace(dataspace)
            .schemaSet(schemaSet)
            .build();

        try {
            fragmentRepository.save(anchor);
        } catch (final DataIntegrityViolationException e) {
            throw new CpsValidationException("Anchor already defined.",
                String.format("Anchor with name %s already defined for dataspace %s.", anchorName, dataspaceName));
        }
    }
}
