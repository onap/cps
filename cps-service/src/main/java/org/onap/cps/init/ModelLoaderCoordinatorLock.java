/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelLoaderCoordinatorLock {

    @Qualifier("cpsCommonLocks")
    private final IMap<String, String> cpsCommonLocks;

    private static final String MODULE_LOADER_LOCK_NAME = "modelLoaderLock";

    boolean tryLock() {
        return cpsCommonLocks.tryLock(MODULE_LOADER_LOCK_NAME);
    }

    boolean isLocked() {
        return cpsCommonLocks.isLocked(MODULE_LOADER_LOCK_NAME);
    }

    void unlock() {
        cpsCommonLocks.forceUnlock(MODULE_LOADER_LOCK_NAME);
    }

}
