/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.spi.repository;

import java.util.Collection;

/**
 * This interface is used in delete fragment entity by id with child using native sql queries.
 */
public interface FragmentNativeRepository {
    void deleteFragmentEntity(long fragmentEntityId);

    /**
     * Delete fragment entities for each supplied xpath.
     * This method will delete list elements or other data nodes, but not whole lists.
     * Non-existing xpaths will not result in an exception.
     * @param anchorId the id of the anchor
     * @param xpaths   xpaths of data nodes to remove
     */
    void deleteByAnchorIdAndXpaths(int anchorId, Collection<String> xpaths);

    /**
     * Delete fragment entities whose xpaths match the given SQL LIKE patterns.
     * For example, if xpath pattern '/parent/list[%' is provided, then all elements in '/parent/list' will be deleted,
     * e.g. /parent/list[@key='A'], /parent/list[@key='B'].
     * Non-existing xpaths will not result in an exception.
     * @param anchorId   the id of the anchor
     * @param xpathPatterns xpath patterns
     */
    void deleteByAnchorIdAndXpathPatterns(int anchorId, Collection<String> xpathPatterns);
}
