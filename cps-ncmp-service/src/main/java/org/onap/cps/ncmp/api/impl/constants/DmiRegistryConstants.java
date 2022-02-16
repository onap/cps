/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.api.impl.constants;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * DmiRegistryConstants class to be strictly used for DMI Related constants only.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DmiRegistryConstants {

    public static final String NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME = "NFP-Operational";

    public static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    public static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    public static final String NCMP_DMI_REGISTRY_PARENT = "/dmi-registry";

    public static final OffsetDateTime NO_TIMESTAMP = null;

    // valid kafka topic name regex
    public static final Pattern TOPIC_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]([._-](?![._-])|"
            + "[a-zA-Z0-9]){0,120}[a-zA-Z0-9]$");

    public static final String NO_TOPIC = null;

    public static final String NO_REQUEST_ID = null;

    public static final String DMI_EXCEPTION_MESSAGE = "Not able to get resource data.";
}
