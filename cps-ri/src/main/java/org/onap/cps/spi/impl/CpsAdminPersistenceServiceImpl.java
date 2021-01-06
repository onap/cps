/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import java.util.Collection;
import java.util.stream.Collectors;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.entities.SchemaSet;
import org.onap.cps.spi.exceptions.AnchorAlreadyDefinedException;
import org.onap.cps.spi.exceptions.DataspaceAlreadyDefinedException;
import org.onap.cps.spi.model.Anchor;
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
    private FragmentRepository fragmentRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Override
    public void createDataspace(final String dataspaceName) {
        try {
            dataspaceRepository.save(new Dataspace(dataspaceName));
        } catch (final DataIntegrityViolationException e) {
            throw new DataspaceAlreadyDefinedException(dataspaceName, e);
        }
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final SchemaSet schemaSet = schemaSetRepository.getByDataspaceAndName(dataspace, schemaSetName);
        final Fragment anchor = Fragment.builder()
            .xpath(anchorName)
            .anchorName(anchorName)
            .dataspace(dataspace)
            .schemaSet(schemaSet)
            .build();
        try {
            fragmentRepository.save(anchor);
        } catch (final DataIntegrityViolationException e) {
            throw new AnchorAlreadyDefinedException(dataspaceName, anchorName, e);
        }
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final Collection<Fragment> fragments = fragmentRepository.findFragmentsThatAreAnchorsByDataspace(dataspace);
        return fragments.stream().map(
            entity -> convertToAnchor(entity)).collect(Collectors.toList());
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final Fragment anchor =
            fragmentRepository.getByDataspaceAndAnchorName(dataspace, anchorName);
        return convertToAnchor(anchor);
    }

    private Anchor convertToAnchor(final Fragment fragmentEntity) {
        return Anchor.builder().name(fragmentEntity.getAnchorName())
            .dataspaceName(fragmentEntity.getDataspace().getName())
            .schemaSetName(fragmentEntity.getSchemaSet().getName()).build();
    }
}