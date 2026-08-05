/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

package org.onap.cps.utils;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension of Gson's {@link JsonReader} that tracks additional parsing context
 * while reading JSON content.
 *
 * <p>Besides the standard streaming parsing capabilities, this reader records
 * the last string/null value encountered and the field names seen within the
 * current object scope. This information can be used by validation and error
 * handling logic to provide more meaningful diagnostics and context when JSON
 * parsing or processing fails.</p>
 */
class TrackingJsonReader extends JsonReader {

    private String lastStringValue;
    private boolean lastValueNull;
    private final Deque<Set<String>> seenFieldNamesStack = new ArrayDeque<>();

    TrackingJsonReader(final Reader reader) {
        super(reader);
    }

    String getLastStringValue() {
        return lastStringValue;
    }

    boolean isLastValueNull() {
        return lastValueNull;
    }

    Set<String> getSeenFieldNames() {
        return seenFieldNamesStack.isEmpty() ? Set.of() : seenFieldNamesStack.getFirst();
    }

    @Override
    public void beginObject() throws IOException {
        super.beginObject();
        seenFieldNamesStack.push(new HashSet<>());
    }

    @Override
    public String nextName() throws IOException {
        final String fieldName = super.nextName();
        seenFieldNamesStack.getFirst().add(fieldName);
        return fieldName;
    }

    @Override
    public void endObject() throws IOException {
        super.endObject();
        seenFieldNamesStack.pop();
    }

    @Override
    public String nextString() throws IOException {
        lastStringValue = super.nextString();
        lastValueNull = false;
        return lastStringValue;
    }

    @Override
    public void nextNull() throws IOException {
        lastStringValue = null;
        lastValueNull = true;
        super.nextNull();
    }
}

