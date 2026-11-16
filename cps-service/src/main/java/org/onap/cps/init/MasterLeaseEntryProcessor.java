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

package org.onap.cps.init;

import com.hazelcast.map.EntryProcessor;
import java.util.Map;

/**
 * Atomically evaluates and updates a master-election lease held in a single shared-map entry. Because an entry
 * processor runs on the Hazelcast partition that owns the key, the read-check-write is atomic across the cluster, so
 * there is never more than one master for a given lease.
 *
 * <p>The lease value is "{@code <instanceId>:<expiryEpochMillis>}". This instance becomes (or stays) master when the
 * entry is empty, already owned by this instance (a renewal), or the current lease has expired (a take-over).
 * Otherwise another instance holds a valid lease and this instance is not the master.
 *
 * <p>Ownership is by instance id rather than by thread, so the lease renews correctly regardless of which thread
 * invokes it.
 */
public class MasterLeaseEntryProcessor implements EntryProcessor<String, String, Boolean> {

    private final String instanceId;
    private final String leaseIfOwned;

    /**
     * Creates a processor for the given instance.
     *
     * @param instanceId   the id of the instance attempting to acquire, renew or take over the lease
     * @param leaseIfOwned the lease value to write when this instance becomes (or stays) master
     */
    public MasterLeaseEntryProcessor(final String instanceId, final String leaseIfOwned) {
        this.instanceId = instanceId;
        this.leaseIfOwned = leaseIfOwned;
    }

    @Override
    public Boolean process(final Map.Entry<String, String> leaseEntry) {
        final String currentLease = leaseEntry.getValue();
        if (currentLease == null || isOwnedByThisInstance(currentLease) || hasExpired(currentLease)) {
            leaseEntry.setValue(leaseIfOwned);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private boolean isOwnedByThisInstance(final String currentLease) {
        return currentLease.startsWith(instanceId + ":");
    }

    private boolean hasExpired(final String currentLease) {
        final long expiry = Long.parseLong(currentLease.substring(currentLease.lastIndexOf(':') + 1));
        return System.currentTimeMillis() > expiry;
    }
}
