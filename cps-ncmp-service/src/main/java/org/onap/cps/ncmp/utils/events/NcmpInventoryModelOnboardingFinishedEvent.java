/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.ncmp.utils.events;

import org.springframework.context.ApplicationEvent;

/**
 * Custom event triggered when the NCMP inventory model onboarding process is completed.
 * Extends Spring's {@link ApplicationEvent} to be used within Spring's event-driven architecture.
 */
public class NcmpInventoryModelOnboardingFinishedEvent extends ApplicationEvent {

    /**
     * Creates a new instance of NcmpModelOnboardingFinishedEvent.
     *
     * @param source the object that is the source of the event (i.e. the source that completed the onboarding process)
     */
    public NcmpInventoryModelOnboardingFinishedEvent(final Object source) {
        super(source);
    }
}
