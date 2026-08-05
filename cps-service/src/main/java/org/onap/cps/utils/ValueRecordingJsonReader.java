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

class ValueRecordingJsonReader extends JsonReader {

    private String lastStringValue;
    private boolean lastValueNull;
    private final Deque<Set<String>> seenFieldNamesStack = new ArrayDeque<>();

    ValueRecordingJsonReader(final Reader reader) {
        super(reader);
    }

    String getLastStringValue() {
        return lastStringValue;
    }

    boolean isLastValueNull() {
        return lastValueNull;
    }

    Set<String> getSeenFieldNames() {
        return seenFieldNamesStack.isEmpty() ? Set.of() : seenFieldNamesStack.peek();
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

