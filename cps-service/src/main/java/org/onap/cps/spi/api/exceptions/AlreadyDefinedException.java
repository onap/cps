/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.spi.api.exceptions;

import java.util.Collection;
import java.util.Collections;
import lombok.Getter;

/**
 * Already defined exception. Indicates the cps object with same name already exists.
 */

@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class AlreadyDefinedException extends CpsAdminException {

    private static final long serialVersionUID = 501929839139881112L;
    public static final String ALREADY_DEFINED_EXCEPTION_MESSAGE = "Already defined exception";

    @Getter
    private final Collection<String> alreadyDefinedObjectNames;

    private AlreadyDefinedException(final String objectType, final String objectName, final String contextName,
        final Throwable cause) {
        super(ALREADY_DEFINED_EXCEPTION_MESSAGE,
            String.format("%s with name %s already exists for %s.", objectType, objectName, contextName), cause);
        alreadyDefinedObjectNames = Collections.singletonList(objectName);
    }

    private AlreadyDefinedException(final String objectType, final Collection<String> objectNames,
                                    final String contextName) {
        super(ALREADY_DEFINED_EXCEPTION_MESSAGE,
                String.format("%d %s already exist for %s.", objectNames.size(), objectType, contextName));
        alreadyDefinedObjectNames = objectNames;
    }

    private AlreadyDefinedException(final String objectName, final Throwable cause) {
        super(ALREADY_DEFINED_EXCEPTION_MESSAGE, String.format("%s already exists.", objectName), cause);
        alreadyDefinedObjectNames = Collections.singletonList(objectName);
    }

    public static AlreadyDefinedException forDataspace(final String dataspaceName, final Throwable cause) {
        return new AlreadyDefinedException(dataspaceName, cause);
    }

    public static AlreadyDefinedException forAnchor(final String anchorName, final String contextName,
        final Throwable cause) {
        return new AlreadyDefinedException("Anchor", anchorName, contextName, cause);
    }

    public static AlreadyDefinedException forSchemaSet(final String schemaSetName, final String contextName,
        final Throwable cause) {
        return new AlreadyDefinedException("Schema Set", schemaSetName, contextName, cause);
    }

    public static AlreadyDefinedException forDataNodes(final Collection<String> xpaths, final String contextName) {
        return new AlreadyDefinedException("data node(s)", xpaths, contextName);
    }
}
