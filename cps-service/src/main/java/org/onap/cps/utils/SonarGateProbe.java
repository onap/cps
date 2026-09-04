/*
 * ============LICENSE_START=======================================================
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

/*
 * ============================================================================
 *  TEMPORARY - CI QUALITY GATE TEST ONLY - DO NOT MERGE - DELETE BEFORE REVIEW
 *  Purpose: raise a Sonar Security Hotspot (java:S2245 - use of java.util.Random
 *  in a security-sensitive context) so the "All new Security Hotspots are
 *  reviewed" quality-gate condition FAILS, while still PASSING `mvn clean
 *  install` (compiles, clears Checkstyle + SpotBugs, and is fully covered by
 *  SonarGateProbeSpec so the JaCoCo 100% gate is satisfied).
 * ============================================================================
 */

package org.onap.cps.utils;

import java.util.Random;

public class SonarGateProbe {

    /**
     * Generates a numeric token using java.util.Random. Sonar flags the use of
     * a non-cryptographic PRNG in a token-generation context as a Security
     * Hotspot (java:S2245) requiring review. Fully covered by tests so the
     * local JaCoCo coverage gate still passes.
     *
     * @param length number of digits in the generated token
     * @return the generated numeric token as a string
     */
    public String generateToken(final int length) {
        final Random random = new Random();
        final StringBuilder token = new StringBuilder();
        for (int i = 0; i < length; i++) {
            token.append(random.nextInt(10));
        }
        return token.toString();
    }
}
