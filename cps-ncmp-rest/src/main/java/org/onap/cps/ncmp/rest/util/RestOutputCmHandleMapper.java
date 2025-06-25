/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestOutputCmHandleMapper {

    private final CmHandleStateMapper cmHandleStateMapper;

    /**
     * Map NcmpServiceCmHandle to a RestOutputCmHandle object.
     *
     * @param ncmpServiceCmHandle            DMI plugin identifier
     * @param includeAdditionalProperties       Boolean for cm handle reference type either
     *                                       cm handle id (False) or alternate id (True)
     * @return                               list of cm handles
     */
    public RestOutputCmHandle toRestOutputCmHandle(final NcmpServiceCmHandle ncmpServiceCmHandle,
                                                   final boolean includeAdditionalProperties) {
        final RestOutputCmHandle restOutputCmHandle = new RestOutputCmHandle();
        restOutputCmHandle.setCmHandle(ncmpServiceCmHandle.getCmHandleId());
        restOutputCmHandle.setPublicCmHandleProperties(
                Collections.singletonList(ncmpServiceCmHandle.getPublicProperties()));
        if (includeAdditionalProperties) {
            restOutputCmHandle.setCmHandleProperties(ncmpServiceCmHandle.getAdditionalProperties());
        } else {
            restOutputCmHandle.setCmHandleProperties(null);
        }
        restOutputCmHandle.setState(
            cmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(ncmpServiceCmHandle.getCompositeState()));
        if (ncmpServiceCmHandle.getCurrentTrustLevel() != null) {
            restOutputCmHandle.setTrustLevel(ncmpServiceCmHandle.getCurrentTrustLevel().toString());
        }
        restOutputCmHandle.setModuleSetTag(ncmpServiceCmHandle.getModuleSetTag());
        restOutputCmHandle.setAlternateId(ncmpServiceCmHandle.getAlternateId());
        restOutputCmHandle.setDataProducerIdentifier(ncmpServiceCmHandle.getDataProducerIdentifier());
        return restOutputCmHandle;
    }
}
