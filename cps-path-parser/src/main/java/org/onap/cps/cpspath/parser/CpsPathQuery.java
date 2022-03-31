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

package org.onap.cps.cpspath.parser;

import static org.onap.cps.cpspath.parser.CpsPathPrefixType.ABSOLUTE;

import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
public class CpsPathQuery {

    private String xpathPrefix;
    private String normalizedXpath;
    private CpsPathPrefixType cpsPathPrefixType = ABSOLUTE;
    private String descendantName;
    private Map<String, Object> leavesData;
    private String ancestorSchemaNodeIdentifier = "";
    private String textFunctionConditionLeafName;
    private String textFunctionConditionValue;

    /**
     * Returns a cps path query.
     *
     * @param cpsPathSource cps path
     * @return a CpsPathQuery object.
     */
    public static CpsPathQuery createFrom(final String cpsPathSource) {
        return CpsPathUtil.getCpsPathQuery(cpsPathSource);
    }

    /**
     * Has ancestor axis been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasAncestorAxis() {
        return !(ancestorSchemaNodeIdentifier.isEmpty());
    }

    /**
     * Have leaf value conditions been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasLeafConditions() {
        return leavesData != null;
    }

    /**
     * Has text function condition been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasTextFunctionCondition() {
        return textFunctionConditionLeafName != null;
    }

}
