/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.models.datajob;

import java.net.URI;

/**
 * Holds the data job response information.
 * based on <a href="https://www.etsi.org/deliver/etsi_ts/128500_128599/128532/16.04.00_60/ts_128532v160400p.pdf">ETSI TS 128 532 V16.4.0 (2020-08)</a>
 *
 * @param jobId      Identifier of the data job.
 * @param status     Current status of data job.
 *                   e.g.  NOT_STARTED, RUNNING, FINISHED, FAILED, PARTIALLY_FAILED, CANCELLING, CANCELLED etc.
 * @param statusUri  Current status of data job as URI.
 * @param resultsUri Current result of data job as URI.
 */
public record Response(String jobId, String status, URI statusUri, URI resultsUri) {}
