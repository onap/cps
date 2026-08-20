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

package org.onap.cps.impl.cache;

import com.hazelcast.topic.ITopic;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.impl.YangTextSchemaSourceSetCache;
import org.springframework.stereotype.Component;

/**
 * Subscribes to the distributed yangSchema cache eviction topic and evicts the local cache entry when a message
 * is received. This ensures all instances clear their parsed-schema cache after a module content refresh on any
 * instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YangSchemaCacheEvictionListener {

    private final ITopic<String> yangSchemaCacheEvictionTopic;
    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    /**
     * Register the message listener on startup.
     */
    @PostConstruct
    public void subscribeToEvictionTopic() {
        yangSchemaCacheEvictionTopic.addMessageListener(message -> {
            final String cacheKey = message.getMessageObject();
            final String[] parts = cacheKey.split("-", 2);
            if (parts.length == 2) {
                final String dataspaceName = parts[0];
                final String schemaSetName = parts[1];
                log.debug("Received yangSchema cache eviction signal for {}-{}", dataspaceName, schemaSetName);
                yangTextSchemaSourceSetCache.removeFromCache(dataspaceName, schemaSetName);
            } else {
                log.warn("Received malformed yangSchema cache eviction key: {}", cacheKey);
            }
        });
        log.info("Subscribed to yangSchema cache eviction topic");
    }
}
