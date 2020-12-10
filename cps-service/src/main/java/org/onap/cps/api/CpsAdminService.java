/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.api;

import java.util.Collection;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.model.Anchor;

/**
 * CPS Admin Service.
 */
public interface CpsAdminService {

    /**
     * Create an anchor using provided anchorDetails object.
     *
     * @param anchor the anchor details object.
     * @return the anchor name.
     * @throws CpsException if input data is invalid.
     */
    String createAnchor(Anchor anchor);

    /**
     * Read all anchors in the given a dataspace.
     *
     * @param dataspaceName dataspace name
     * @return a collection of anchors
     */
    Collection<Anchor> getAnchors(String dataspaceName);
}
