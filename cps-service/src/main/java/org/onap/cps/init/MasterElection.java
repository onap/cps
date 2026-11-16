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

import com.hazelcast.map.IMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Reusable single-master election for tasks that must run on exactly one instance at a time (for example module sync).
 *
 * <p>Election uses a value-based lease ("{@code <instanceId>:<expiryEpochMillis>}") stored per named key in the shared
 * cpsCommonLocks map and evaluated atomically by {@link MasterLeaseEntryProcessor}, so there is never more than one
 * master for a given lease. A caller becomes master by calling {@link #isMaster(String)}; while it remains master this
 * service renews the lease on a fixed heartbeat, independently of the (possibly long-running) work the caller performs,
 * so a healthy master never loses the lease mid-task. If a master stops heartbeating (crash/hang) its lease expires and
 * another instance can take over.
 *
 * <p>Ownership is by instance id, not by thread, so renewal works regardless of which scheduler thread runs it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterElection {

    @Qualifier("cpsCommonLocks")
    private final IMap<String, String> cpsCommonLocks;

    @Qualifier("cpsInstanceId")
    private final String cpsInstanceId;

    // The lease must be comfortably longer than the heartbeat so a healthy master always renews in time, while short
    // enough that a surviving instance takes over promptly if the master dies.
    static final long LEASE_DURATION_MILLIS = 30_000L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 10_000L;

    private final Set<String> leaseNamesHeldByThisInstance = ConcurrentHashMap.newKeySet();

    /**
     * Determines whether this instance is the master for the given lease, acquiring or taking over the lease when it
     * is free or expired. While this instance remains master the lease is kept alive by the heartbeat, so callers only
     * need to ask again each time they are about to do work.
     *
     * @param leaseName the name of the lease (allows several independent masters for different responsibilities)
     * @return true if this instance is the master for the lease
     */
    public boolean isMaster(final String leaseName) {
        final boolean isMaster = tryAcquireOrRenew(leaseName);
        if (isMaster) {
            leaseNamesHeldByThisInstance.add(leaseName);
        } else {
            leaseNamesHeldByThisInstance.remove(leaseName);
        }
        return isMaster;
    }

    /**
     * Heartbeat that renews every lease currently held by this instance so a healthy master keeps its leases alive
     * regardless of how long its work takes. A lease that can no longer be renewed (taken over by another instance)
     * is dropped from the held set and a warning is logged. Runs on a fixed interval shorter than the lease duration.
     */
    @Scheduled(fixedDelay = HEARTBEAT_INTERVAL_MILLIS)
    public void renewHeldLeases() {
        for (final String leaseName : leaseNamesHeldByThisInstance) {
            if (!tryAcquireOrRenew(leaseName)) {
                leaseNamesHeldByThisInstance.remove(leaseName);
                log.warn("Lost master lease '{}' for instance {}", leaseName, cpsInstanceId);
            }
        }
    }

    private boolean tryAcquireOrRenew(final String leaseName) {
        final long expiry = System.currentTimeMillis() + LEASE_DURATION_MILLIS;
        final String leaseIfOwned = cpsInstanceId + ":" + expiry;
        // Returns true when this instance acquired, renewed, or took over the lease (i.e. it is now the master).
        return Boolean.TRUE.equals(
                cpsCommonLocks.executeOnKey(leaseName, new MasterLeaseEntryProcessor(cpsInstanceId, leaseIfOwned)));
    }
}
