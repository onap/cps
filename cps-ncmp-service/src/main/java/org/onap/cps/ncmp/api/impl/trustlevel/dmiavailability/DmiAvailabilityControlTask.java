/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.trustlevel.dmiavailability;

import com.hazelcast.map.IMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;

@Slf4j
@AllArgsConstructor
public class DmiAvailabilityControlTask implements Runnable {

    private final IMap<String, Map<String, String>> dmiPluginTrustLevelCache;

    private final DmiRestClient dmiRestClient;

    @Override
    public void run() {
        checkDmiPluginsAvailability();
    }

    private void checkDmiPluginsAvailability() {
        dmiPluginTrustLevelCache.forEach((dmiPluginName, trustLevelPropertiesMap) -> {
            final String dmiPluginHealthCheckUrl = trustLevelPropertiesMap.get("healthCheckUrl");
            final DmiPluginStatus dmiPluginStatus = dmiRestClient.getDmiPluginStatus(dmiPluginHealthCheckUrl);
            log.debug("Trust level for dmi-plugin: {} is {}", dmiPluginName, dmiPluginStatus.toString());
            trustLevelPropertiesMap.put("trustLevel", dmiPluginStatus.toString());
        });
    }
}
