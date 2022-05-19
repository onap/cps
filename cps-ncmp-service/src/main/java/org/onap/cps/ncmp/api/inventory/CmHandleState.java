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

package org.onap.cps.ncmp.api.inventory;

public enum CmHandleState {
    ADVISED {
        private LockReasonEnum lockReasonEnum;
        private String details;

        @Override
        public CmHandleState ready() {
            return READY;
        }

        @Override
        public CmHandleState lock(final LockReasonEnum lockReasonEnum, final String lockDetails) {
            this.lockReasonEnum = lockReasonEnum;
            this.details = lockDetails;
            return LOCKED;
        }

        @Override
        public LockReasonEnum valueOfLockReason() {
            return this.lockReasonEnum;
        }

        @Override
        public String valueOfLockDetails() {
            return this.details;
        }
    },
    READY {
        private LockReasonEnum lockReasonEnum;
        private String details;

        @Override
        public CmHandleState ready() {
            return this;
        }

        @Override
        public CmHandleState lock(final LockReasonEnum lockReasonEnum, final String lockDetails) {
            this.lockReasonEnum = lockReasonEnum;
            this.details = lockDetails;
            return LOCKED;
        }

        @Override
        public LockReasonEnum valueOfLockReason() {
            return this.lockReasonEnum;
        }

        @Override
        public String valueOfLockDetails() {
            return this.details;
        }
    },
    LOCKED {

        private LockReasonEnum lockReasonEnum;
        private String details;

        @Override
        public CmHandleState ready() {
            return READY;
        }

        @Override
        public CmHandleState lock(final LockReasonEnum lockReasonEnum, final String lockDetails) {
            this.lockReasonEnum = lockReasonEnum;
            this.details = lockDetails;
            return this;
        }

        @Override
        public LockReasonEnum valueOfLockReason() {
            return this.lockReasonEnum;
        }

        @Override
        public String valueOfLockDetails() {
            return this.details;
        }
    };

    private static String details;

    public abstract CmHandleState ready();

    public abstract CmHandleState lock(final LockReasonEnum lockReasonEnum, final String details);

    public abstract LockReasonEnum valueOfLockReason();

    public abstract String valueOfLockDetails();

    public enum LockReasonEnum {
        LOCKED_MISBEHAVING,
        LOCKED_UPGRADING,
        LOCKED_OTHER
    }

}
