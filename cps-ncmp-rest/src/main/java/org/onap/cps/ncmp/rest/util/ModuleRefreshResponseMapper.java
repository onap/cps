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
import java.util.List;
import java.util.Map;
import org.onap.cps.ncmp.api.inventory.models.RefreshCmHandle;
import org.onap.cps.ncmp.rest.model.CmHandlesByModuleSetTag;
import org.onap.cps.ncmp.rest.model.ModuleRefreshCmHandle;
import org.onap.cps.ncmp.rest.model.RestModuleRefreshResponse;
import org.springframework.stereotype.Component;

@Component
public class ModuleRefreshResponseMapper {

    /**
     * Map the matched CM Handles grouped by module set tag to a RestModuleRefreshResponse object.
     * Each CM Handle carries its reference and current state, and 'selectedForRefresh' marks the single sample
     * (first READY CM Handle) that will be refreshed for the module set tag.
     *
     * @param refreshCmHandlesByModuleSetTag map of module set tag to the matched CM Handles
     * @return the module refresh response, with an empty array when no cm handles matched
     */
    public RestModuleRefreshResponse toRestModuleRefreshResponse(
            final Map<String, List<RefreshCmHandle>> refreshCmHandlesByModuleSetTag) {
        final List<CmHandlesByModuleSetTag> cmHandlesByModuleSetTagList = new ArrayList<>();
        refreshCmHandlesByModuleSetTag.forEach((moduleSetTag, refreshCmHandles) -> {
            final CmHandlesByModuleSetTag cmHandlesByModuleSetTag = new CmHandlesByModuleSetTag();
            cmHandlesByModuleSetTag.setModuleSetTag(moduleSetTag);
            final List<ModuleRefreshCmHandle> moduleRefreshCmHandles = new ArrayList<>(refreshCmHandles.size());
            for (final RefreshCmHandle refreshCmHandle : refreshCmHandles) {
                final ModuleRefreshCmHandle moduleRefreshCmHandle = new ModuleRefreshCmHandle();
                moduleRefreshCmHandle.setCmHandleReference(refreshCmHandle.cmHandleReference());
                moduleRefreshCmHandle.setCmHandleState(refreshCmHandle.cmHandleState());
                moduleRefreshCmHandle.setSelectedForRefresh(refreshCmHandle.selectedForRefresh());
                moduleRefreshCmHandles.add(moduleRefreshCmHandle);
            }
            cmHandlesByModuleSetTag.setCmHandles(moduleRefreshCmHandles);
            cmHandlesByModuleSetTagList.add(cmHandlesByModuleSetTag);
        });
        final RestModuleRefreshResponse restModuleRefreshResponse = new RestModuleRefreshResponse();
        restModuleRefreshResponse.setCmHandlesByModuleSetTag(cmHandlesByModuleSetTagList);
        return restModuleRefreshResponse;
    }
}
