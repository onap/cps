/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
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

package org.onap.cps.cpspath.parser;

/**
 * The enum Cps path query type.
 */
public enum CpsPathQueryType {
    /**
     * Xpath descendant anywhere type e.g. //nodeName .
     */
    XPATH_HAS_DESCENDANT_ANYWHERE,
    /**
     * Xpath descendant anywhere type e.g. //nodeName[@leafName=leafValue] .
     */
    XPATH_HAS_DESCENDANT_WITH_LEAF_VALUES,
    /**
     * Xpath leaf value cps path query type e.g. /cps-path[@leafName=leafValue] .
     */
    XPATH_LEAF_VALUE
}
