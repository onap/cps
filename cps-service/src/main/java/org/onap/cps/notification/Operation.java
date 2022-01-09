/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada.
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

package org.onap.cps.notification;

import lombok.Getter;

public enum Operation {

    ROOT_NODE_CREATE("create"),
    ROOT_NODE_UPDATE(),
    ROOT_NODE_DELETE("delete"),
    CHILD_NODE_CREATE(),
    CHILD_NODE_UPDATE(),
    CHILD_NODE_DELETE();

    @Getter
    private String rootNodeOperation;

    Operation() {
        this("update");
    }

    Operation(final String rootNodeOperation) {
        this.rootNodeOperation = rootNodeOperation;
    }
}
