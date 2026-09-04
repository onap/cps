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
 *  Purpose: verify the SonarQube quality gate FAILS on a new maintainability
 *  issue (java:S3776 Cognitive Complexity > 15) while still PASSING
 *  `mvn clean install` (compiles, clears Checkstyle + SpotBugs, and is fully
 *  covered by SonarGateProbeSpec so the JaCoCo 100% gate is satisfied).
 * ============================================================================
 */

package org.onap.cps.utils;

public class SonarGateProbe {

    /**
     * Deliberately high cognitive complexity (nested branches and loops) to
     * exceed Sonar's default threshold of 15 for rule java:S3776. Fully
     * covered by tests so the local JaCoCo coverage gate still passes.
     *
     * @param value input value to classify
     * @return a classification string
     */
    public String classify(final int value) {
        if (value > 0) {
            if (value > 10) {
                if (value > 100) {
                    if (value > 1000) {
                        return "gigantic";
                    } else {
                        return "huge";
                    }
                } else if (value > 50) {
                    return "large";
                } else {
                    return "big";
                }
            } else if (value > 5) {
                return "medium";
            } else {
                return "small";
            }
        } else if (value < 0) {
            if (value < -100) {
                if (value < -1000) {
                    return "hugely-negative";
                } else {
                    return "very-negative";
                }
            } else if (value < -10) {
                return "quite-negative";
            } else {
                return "negative";
            }
        } else {
            return "zero";
        }
    }
}
