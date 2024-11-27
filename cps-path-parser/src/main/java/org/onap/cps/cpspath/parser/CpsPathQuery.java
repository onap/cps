/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd
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

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
public class CpsPathQuery {

    private String xpathPrefix;
    private String normalizedParentPath;
    private String normalizedXpath;
    private List<String> containerNames;
    private CpsPathPrefixType cpsPathPrefixType = ABSOLUTE;
    private String descendantName;
    private List<LeafCondition> leafConditions;
    private String ancestorSchemaNodeIdentifier = "";
    private String attributeName = "";
    private String textFunctionConditionLeafName;
    private String textFunctionConditionValue;
    private List<String> booleanOperators;
    private String containsFunctionConditionLeafName;
    private String containsFunctionConditionValue;

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
     * Has attribute axis been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasAttributeAxis() {
        return !(attributeName.isEmpty());
    }

    /**
     * Have leaf value conditions been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasLeafConditions() {
        return leafConditions != null;
    }

    /**
     * Has text function condition been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasTextFunctionCondition() {
        return textFunctionConditionLeafName != null;
    }

    /**
     * Has contains function condition been included in cpsPath.
     *
     * @return boolean value.
     */
    public boolean hasContainsFunctionCondition() {
        return containsFunctionConditionLeafName != null;
    }

    /**
     * Returns boolean indicating xpath is an absolute path to a list element.
     *
     * @return true if xpath is an absolute path to a list element
     */
    public boolean isPathToListElement() {
        return cpsPathPrefixType == ABSOLUTE && hasLeafConditions();
    }

    public record LeafCondition(String name, String operator, Object value) { }

}
