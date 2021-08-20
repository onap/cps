/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import lombok.Getter;

@JsonInclude(Include.NON_NULL)
@Getter
public class GenericRequestBody   {
    public enum OperationEnum {
        READ("read"),
        CREATE("create");
        private String value;

        OperationEnum(final String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    private OperationEnum operation;
    private String dataType;
    private Object data;
    private Map<String, String> cmHandleProperties;

    private GenericRequestBody(final OperationEnum operation, final String dataType,
                               final Object data, final Map<String, String> cmHandleProperties) {
        this.operation = operation;
        this.dataType = dataType;
        this.data = data;
        this.cmHandleProperties = cmHandleProperties;
    }

    public static class Builder {
        private OperationEnum operation;
        private String dataType;
        private Object data;
        private Map<String, String> cmHandleProperties;

        public Builder setOperation(final OperationEnum operation) {
            this.operation = operation;
            return this;
        }

        public Builder setDataType(final String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder setData(final Object data) {
            this.data = data;
            return this;
        }

        public Builder setCmHandleProperties(final Map<String, String> cmHandleProperties) {
            this.cmHandleProperties = cmHandleProperties;
            return this;
        }

        public GenericRequestBody build() {
            return new GenericRequestBody(operation, dataType, data, cmHandleProperties);
        }
    }
}
