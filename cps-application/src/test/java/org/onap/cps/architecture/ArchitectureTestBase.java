/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.architecture;

import org.apache.commons.lang3.ArrayUtils;

public class ArchitectureTestBase {

    private static final String[] ACCEPTED_3PP_PACKAGES = { "com.fasterxml..",
                                                            "com.google..",
                                                            "com.hazelcast..",
                                                            "edu..",
                                                            "io.cloudevents..",
                                                            "io.micrometer..",
                                                            "io.netty..",
                                                            "io.swagger..",
                                                            "jakarta..",
                                                            "java..",
                                                            "lombok..",
                                                            "org.apache..",
                                                            "org.mapstruct..",
                                                            "org.opendaylight..",
                                                            "org.slf4j..",
                                                            "org.springframework..",
                                                            "reactor.."
    };

    static String[] commonAndListedPackages(final String... packageIdentifiers) {
        return ArrayUtils.addAll(ACCEPTED_3PP_PACKAGES, packageIdentifiers);
    }
}
