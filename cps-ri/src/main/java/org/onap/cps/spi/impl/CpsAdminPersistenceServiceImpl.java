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

import java.lang.reflect.Type;
import java.util.Collection;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.onap.cps.exceptions.CpsNotFoundException;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Fragment;
import org.onap.cps.spi.entities.Module;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.repository.ModuleRepository;
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
    private ModuleRepository moduleRepository;

    @Override
    public String createAnchor(final Anchor anchor) {
        final String anchorName = anchor.getAnchorName();
        try {
            final Dataspace dataspace = dataspaceRepository.getByName(anchor.getDataspaceName());
            final Module module =
                moduleRepository.getByDataspaceAndNamespaceAndRevision(dataspace,
                    anchor.getNamespace(), anchor.getRevision());

            final Fragment fragment = Fragment.builder().xpath(anchorName)
                .anchorName(anchorName)
                .dataspace(dataspace).module(module).build();

            fragmentRepository.save(fragment);
            return anchorName;
        } catch (final CpsNotFoundException ex) {
            throw new CpsValidationException("Validation Error",
                "Dataspace and/or Module do not exist.");
        } catch (final DataIntegrityViolationException ex) {
            throw new CpsValidationException("Duplication Error",
                String.format("Anchor with name %s already exist in dataspace %s.",
                    anchor.getAnchorName(), anchor.getDataspaceName()));
        }
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final Collection<Fragment> fragments = fragmentRepository.findFragmentsByDataspace(dataspace);
        final Type anchorListType = new TypeToken<Collection<Anchor>>() {}.getType();
        return new ModelMapper().map(fragments, anchorListType);
    }
}
