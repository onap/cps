/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.api.inventory.models;

/**
 * A matched CM Handle in a module refresh group.
 * Carries the display reference (alternate id or cm handle id) and current state, and marks whether this CM Handle
 * is the single sample (first READY) selected to be refreshed for its module set tag. The cm handle id is retained
 * so the selected sample can later be locked for refresh.
 *
 * @param cmHandleId         the cm handle id
 * @param cmHandleReference  the alternate id where available, otherwise the cm handle id
 * @param cmHandleState      the current cm handle state
 * @param selectedForRefresh true for the single sample (first READY) selected to be refreshed for the tag
 */
public record RefreshCmHandle(String cmHandleId, String cmHandleReference, String cmHandleState,
                              boolean selectedForRefresh) {
}
