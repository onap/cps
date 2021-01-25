/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

package org.onap.cps.utils;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class YangUtils {

    @Autowired
    CpsDataNodeBuilder cpsDataNodeBuilder;

    private YangUtils() {
        cpsDataNodeBuilder  = new CpsDataNodeBuilder();
    }

    /**
     * Parse a string containing json data for a certain model (schemaContext).
     *
     * @param jsonData      a string containing json data for the given model
     * @param schemaContext the SchemaContext for the given data
     * @return the NormalizedNode representing the json data
     */
    public static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext)
            throws IOException {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .getShared(schemaContext);
        final NormalizedNodeResult normalizedNodeResult = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
                .from(normalizedNodeResult);
        try (final JsonParserStream jsonParserStream = JsonParserStream
                .create(normalizedNodeStreamWriter, jsonCodecFactory)) {
            final JsonReader jsonReader = new JsonReader(new StringReader(jsonData));
            jsonParserStream.parse(jsonReader);
        }
        return normalizedNodeResult.getResult();
    }

}
