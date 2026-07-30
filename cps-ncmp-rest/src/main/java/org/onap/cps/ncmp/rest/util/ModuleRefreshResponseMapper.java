/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.rest.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.onap.cps.ncmp.rest.model.CmHandlesByModuleSetTag;
import org.onap.cps.ncmp.rest.model.RestModuleRefreshResponse;
import org.springframework.stereotype.Component;

@Component
public class ModuleRefreshResponseMapper {

    /**
     * Map matched cm handle references grouped by module set tag to a RestModuleRefreshResponse object.
     *
     * @param cmHandleReferencesByModuleSetTag map of module set tag to the collection of cm handle references
     * @return the module refresh response, with an empty array when no cm handles matched
     */
    public RestModuleRefreshResponse toRestModuleRefreshResponse(
            final Map<String, Collection<String>> cmHandleReferencesByModuleSetTag) {
        final List<CmHandlesByModuleSetTag> cmHandlesByModuleSetTagList = new ArrayList<>();
        cmHandleReferencesByModuleSetTag.forEach((moduleSetTag, cmHandleReferences) -> {
            final CmHandlesByModuleSetTag cmHandlesByModuleSetTag = new CmHandlesByModuleSetTag();
            cmHandlesByModuleSetTag.setModuleSetTag(moduleSetTag);
            cmHandlesByModuleSetTag.setCmHandles(new ArrayList<>(cmHandleReferences));
            cmHandlesByModuleSetTagList.add(cmHandlesByModuleSetTag);
        });
        final RestModuleRefreshResponse restModuleRefreshResponse = new RestModuleRefreshResponse();
        restModuleRefreshResponse.setCmHandlesByModuleSetTag(cmHandlesByModuleSetTagList);
        return restModuleRefreshResponse;
    }
}
