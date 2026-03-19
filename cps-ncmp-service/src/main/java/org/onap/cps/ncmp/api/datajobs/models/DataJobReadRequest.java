/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.datajobs.models;

import java.util.Map;

/**
 * Represents a read data job request to be forwarded to a DMI plugin.
 * Contains metadata and configuration for executing read operations on network elements.
 *
 * @param name              the name of the data job
 * @param jobId             the unique identifier for the data job
 * @param description       a description of the data job purpose
 * @param readProperties    the read operation properties including targets and data specifications
 * @param customProperties  additional custom properties for the data job
 */
public record DataJobReadRequest(String name, String jobId, String description,
                                 ReadProperties readProperties,
                                 Map<String, String> customProperties) {}
