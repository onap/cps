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

package org.onap.cps.impl.cache

import com.hazelcast.core.Hazelcast
import com.hazelcast.topic.ITopic
import com.hazelcast.topic.impl.TopicProxy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [YangSchemaCacheEvictionConfig])
@TestPropertySource(properties = ['hazelcast.instance-config-name=yangSchemaCacheEvictionConfigSpecInstance'])
class YangSchemaCacheEvictionConfigSpec extends Specification {

    @Autowired
    ITopic<String> yangSchemaCacheEvictionTopic

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('yangSchemaCacheEvictionConfigSpecInstance').shutdown()
    }

    def 'Yang schema cache eviction topic'() {
        given: 'the hazelcast instance backing the autowired topic'
            def hazelcastInstance = ((TopicProxy) yangSchemaCacheEvictionTopic).getNodeEngine().getHazelcastInstance()
        expect: 'the topic has the correct name'
            assert  yangSchemaCacheEvictionTopic.getName() == 'yangSchemaCacheEvictionTopic'
        and: 'the topic is using the default configuration'
            def config = hazelcastInstance.getConfig().findTopicConfig(yangSchemaCacheEvictionTopic.getName())
            assert config.name == '*'
    }

}
