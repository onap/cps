/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class ServiceConfig {
    private String connectionProviderName = "";
    private int maximumConnectionsTotal = 1;
    private int pendingAcquireMaxCount = 1;
    private Integer connectionTimeoutInSeconds = 1;
    private long readTimeoutInSeconds = 1;
    private long writeTimeoutInSeconds = 1;
    private int maximumInMemorySizeInMegabytes = 1;
}
