/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.spi.exceptions;

/**
 * List Node Exception. Indicates that a list node was found instead of a data node
 */
@SuppressWarnings("squid:S110")
public class ListNodeGivenException extends DataValidationException {

    private static final long serialVersionUID = 6535502366085872674L;

    public ListNodeGivenException(final String dataspaceName, final String anchorName, final String xpath) {
        super("List Node found", String.format("Item under xpath %s for anchor %s and dataspace %s is a list node.",
            xpath, anchorName, dataspaceName));
    }


}
