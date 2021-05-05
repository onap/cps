/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.spi.exceptions;

import java.util.Collection;

/**
 * Already defined exception. Indicates the cps object with same name already exists.
 */

@SuppressWarnings("squid:S110")  // Team agreed to accept 6 levels of inheritance for CPS Exceptions
public class AlreadyDefinedException extends CpsAdminException {

    private static final long serialVersionUID = 501929839139881112L;

    /**
     * Constructor.
     *
     * @param objectType  the object type
     * @param objectName  the name of the object
     * @param contextName the context name e.g. Anchor or Dataspace
     * @param cause       the cause of the exception
     */
    private AlreadyDefinedException(final String objectType, final String objectName, final String contextName,
        final Throwable cause) {
        super("Already defined exception",
            String.format("%s with name %s already exists for %s.", objectType, objectName, contextName), cause);
    }

    /**
     * Constructor.
     *
     * @param objectName the name of the object
     * @param cause      the cause of the exception
     */
    private AlreadyDefinedException(final String objectName, final Throwable cause) {
        super("Already defined exception", String.format("%s already exists.", objectName), cause);
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

    public static AlreadyDefinedException forDataNode(final String xpath, final String contextName,
        final Throwable cause) {
        return new AlreadyDefinedException("Data node", xpath, contextName, cause);
    }

    public static AlreadyDefinedException forDataNodes(final Collection<String> xpaths, final String contextName,
        final Throwable cause) {
        final var name = String.format("(one or more) of %s", xpaths);
        return new AlreadyDefinedException("Data node", name, contextName, cause);
    }
}
