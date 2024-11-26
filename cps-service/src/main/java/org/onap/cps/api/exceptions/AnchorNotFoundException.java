/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.cps.api.exceptions;

@SuppressWarnings("squid:S110") // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class AnchorNotFoundException extends CpsAdminException {

    private static final long serialVersionUID = -1821064664642194882L;

    /**
     * Constructor.
     *
     * @param anchorName the name of the anchor
     * @param dataspaceName the dataspace name
     */
    public AnchorNotFoundException(final String anchorName, final String dataspaceName) {
        super("Anchor not found",
            String.format("Anchor with name %s does not exist in dataspace %s.", anchorName,
                dataspaceName));
    }
}
