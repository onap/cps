/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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

package org.onap.cps.utils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class YangUtils {

    /**
     * Create an xpath form a Yang Tools NodeIdentifier (i.e. PathArgument).
     *
     * @param nodeIdentifier the NodeIdentifier
     * @return a xpath
     */
    public static String buildXpath(final YangInstanceIdentifier.PathArgument nodeIdentifier) {
        final StringBuilder xpathBuilder = new StringBuilder();
        xpathBuilder.append("/").append(nodeIdentifier.getNodeType().getLocalName());

        if (nodeIdentifier instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            xpathBuilder.append(getKeyAttributesStatement(
                (YangInstanceIdentifier.NodeIdentifierWithPredicates) nodeIdentifier));
        }
        return xpathBuilder.toString();
    }

    private static String getKeyAttributesStatement(
            final YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier) {
        final List<String> keyAttributes = nodeIdentifier.entrySet().stream().map(
                entry -> {
                    final String name = entry.getKey().getLocalName();
                    final String value = String.valueOf(entry.getValue()).replace("'", "''");
                    return String.format("@%s='%s'", name, value);
                }
        ).collect(Collectors.toList());

        if (keyAttributes.isEmpty()) {
            return "";
        } else {
            Collections.sort(keyAttributes);
            return "[" + String.join(" and ", keyAttributes) + "]";
        }
    }

}
