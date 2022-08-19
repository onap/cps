/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory;

public enum ModelledDmiServiceLeafNames {
    DMI_SERVICE_NAME("dmi-service-name"),
    DMI_DATA_SERVICE_NAME("dmi-data-service-name"),
    DMI_MODEL_SERVICE_NAME("dmi-model-service-name");

    private String leafName;

    ModelledDmiServiceLeafNames(final String dmiPluginIdentifierKey) {
        this.leafName = dmiPluginIdentifierKey;
    }

    public String getLeafName() {
        return leafName;
    }

}
