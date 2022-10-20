/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.api.impl;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.utils.CpsValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpsQueryServiceImpl implements CpsQueryService {

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsValidator cpsValidator;

    @Override
    public Collection<DataNode> queryDataNodes(final String dataspaceName, final String anchorName,
        final String cpsPath, final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
    }
}
