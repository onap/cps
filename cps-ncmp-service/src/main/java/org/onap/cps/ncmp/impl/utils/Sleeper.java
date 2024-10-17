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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.utils;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * This class is to extract out sleep functionality so the interrupted exception handling can
 * be covered with a test (e.g. using spy on Sleeper) and help to get to 100% code coverage.
 */
@Service
public class Sleeper {
    public void haveALittleRest(final long timeInMillis) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(timeInMillis);
    }
}
