/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.cache;

import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicates;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;

/**
 * Wrapper around Hazelcast IMap that maintains a bidirectional mapping for CM handle references.
 *
 * <p>Stores both cmHandleId->cmHandleId AND alternateId->cmHandleId entries, enabling O(1) lookups
 * for both cm handle ids and alternate ids.
 */
public class CmHandleIdPerReferenceMap {

    private final IMap<String, String> cmHandleIdPerReference;

    public CmHandleIdPerReferenceMap(final IMap<String, String> cmHandleIdPerReference) {
        this.cmHandleIdPerReference = cmHandleIdPerReference;
    }

    /**
     * Add entries for a collection of CM handles.
     * For each CM handle, stores cmHandleId->cmHandleId, and if an alternateId exists,
     * also stores alternateId->cmHandleId.
     *
     * @param yangModelCmHandles collection of CM handles to add
     */
    public void putAll(final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<String, String> entriesToAdd = new HashMap<>(yangModelCmHandles.size() * 2);
        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final String cmHandleId = yangModelCmHandle.getId();
            entriesToAdd.put(cmHandleId, cmHandleId);
            final String alternateId = yangModelCmHandle.getAlternateId();
            if (StringUtils.isNotBlank(alternateId)) {
                entriesToAdd.put(alternateId, cmHandleId);
            }
        }
        cmHandleIdPerReference.putAll(entriesToAdd);
    }

    /**
     * Remove entries for a collection of CM handles.
     * Removes both the cmHandleId and alternateId entries.
     *
     * @param yangModelCmHandles collection of CM handles to remove
     */
    public void removeAll(final Collection<YangModelCmHandle> yangModelCmHandles) {
        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final String cmHandleId = yangModelCmHandle.getId();
            cmHandleIdPerReference.delete(cmHandleId);
            final String alternateId = yangModelCmHandle.getAlternateId();
            if (StringUtils.isNotBlank(alternateId)) {
                cmHandleIdPerReference.delete(alternateId);
            }
        }
    }

    /**
     * Update the alternate id for a CM handle.
     * Removes the old cmHandleId key (which was acting as placeholder) and adds the new alternateId key.
     *
     * @param cmHandleId    the CM handle id
     * @param newAlternateId the new alternate id to map
     */
    public void updateAlternateId(final String cmHandleId, final String newAlternateId) {
        cmHandleIdPerReference.set(cmHandleId, cmHandleId);
        cmHandleIdPerReference.set(newAlternateId, cmHandleId);
    }

    /**
     * Get the cm handle id for a given reference (either cmHandleId or alternateId).
     *
     * @param cmHandleReference the cm handle reference to look up
     * @return the cm handle id, or null if not found
     */
    public String get(final String cmHandleReference) {
        return cmHandleIdPerReference.get(cmHandleReference);
    }

    /**
     * Get all cm handle ids for a given set of references.
     *
     * @param cmHandleReferences the set of references to look up
     * @return map of reference to cm handle id for found entries
     */
    public Map<String, String> getAll(final Set<String> cmHandleReferences) {
        return cmHandleIdPerReference.getAll(cmHandleReferences);
    }

    /**
     * Check if the map contains the given reference key.
     *
     * @param cmHandleReference the reference to check
     * @return true if present
     */
    public boolean containsKey(final String cmHandleReference) {
        return cmHandleIdPerReference.containsKey(cmHandleReference);
    }

    /**
     * Get all values matching a key predicate with LIKE pattern.
     *
     * @param searchTerm the substring to search for in keys
     * @return collection of matching cm handle ids
     */
    public Collection<String> getByKeyLike(final String searchTerm) {
        return cmHandleIdPerReference.values(Predicates.like("__key", "%" + searchTerm + "%"));
    }

    /**
     * Check if the map is empty.
     *
     * @return true if map has no entries
     */
    public boolean isEmpty() {
        return cmHandleIdPerReference.isEmpty();
    }

    /**
     * Get the size of the map.
     *
     * @return number of entries in the map
     */
    public int size() {
        return cmHandleIdPerReference.size();
    }
}
