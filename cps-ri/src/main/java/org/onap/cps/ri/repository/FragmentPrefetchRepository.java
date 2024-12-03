/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

package org.onap.cps.ri.repository;

import java.util.Collection;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.ri.models.FragmentEntity;

public interface FragmentPrefetchRepository {
    Collection<FragmentEntity> prefetchDescendantsOfFragmentEntities(
            final FetchDescendantsOption fetchDescendantsOption,
            final Collection<FragmentEntity> proxiedFragmentEntities);
}
