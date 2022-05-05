/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.inventory.sync;

public enum CmHandleState {

    ADVISED {
        @Override
        public CmHandleState nextState() {
            return READY;
        }

        @Override
        public CmHandleState lock(final Enum<LockReasonEnum> lockReasonEnum, final String details) {
            return LOCKED;
        }

        @Override
        public CmHandleState delete() {
            return DELETING;
        }
    },
    LOCKED {
        @Override
        public CmHandleState nextState() {
            return READY;
        }

        @Override
        public CmHandleState lock(final Enum<LockReasonEnum> lockReasonEnum, final String lockDetails) {
            return this;
        }

        @Override
        public CmHandleState delete() {
            return DELETING;
        }
    },
    READY {
        @Override
        public CmHandleState nextState() {
            return this;
        }

        @Override
        public CmHandleState lock(final Enum<LockReasonEnum> lockReasonEnum, final String details) {
            return LOCKED;
        }

        @Override
        public CmHandleState delete() {
            return DELETING;
        }
    },
    DELETING {
        @Override
        public CmHandleState nextState() {
            return this;
        }

        @Override
        public CmHandleState lock(final Enum<LockReasonEnum> lockReasonEnum, final String details) {
            return this;
        }

        @Override
        public CmHandleState delete() {
            return this;
        }
    };

    public abstract CmHandleState nextState();

    public abstract CmHandleState lock(final Enum<LockReasonEnum> lockReasonEnum, final String details);

    public abstract CmHandleState delete();

    public enum LockReasonEnum {
        LOCKED_MISBEHAVING,
        LOCKED_UPGRADING,
        LOCKED_OTHER
    }

}
