/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 Nordix Foundation
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

package org.onap.cps.ncmp.utils.events;

import java.util.regex.Pattern;
import org.onap.cps.ncmp.api.exceptions.InvalidTopicException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TopicValidator {

    private static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([._-](?![._-])|"
        + "[a-zA-Z0-9]){0,120}[a-zA-Z0-9]$");

    @Value("${app.ncmp.async-m2m.topic}")
    String reservedTopicName;

    /**
     * Validate kafka topic name pattern and that it is not reserved for internal use.
     *
     * @param topicName name of the topic to be validated
     *
     * @throws InvalidTopicException if the topic is invalid or reserved
     */
    public void validateTopicName(final String topicName) {
        if (!TOPIC_NAME_PATTERN.matcher(topicName).matches()) {
            throw new InvalidTopicException("Topic name " + topicName + " is invalid",
                    "invalid topic name, allowed: alphanumeric, '.', '_', '-'");
        }
        if (topicName.equals(reservedTopicName)) {
            throw new InvalidTopicException("Topic name " + topicName + " is invalid", "Topic "
                    + topicName + " is reserved for internal use");
        }
    }
}
