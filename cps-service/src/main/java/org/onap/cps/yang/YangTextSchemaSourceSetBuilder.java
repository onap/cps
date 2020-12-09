/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
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

package org.onap.cps.yang;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onap.cps.spi.model.ModuleReference;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.YangNames;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

public final class YangTextSchemaSourceSetBuilder {

    private final ImmutableMap.Builder<String, String> yangModelMap = new ImmutableMap.Builder<>();

    public YangTextSchemaSourceSetBuilder() {
    }

    public YangTextSchemaSourceSetBuilder put(final String fileName, final String content) {
        this.yangModelMap.put(fileName, content);
        return this;
    }

    public YangTextSchemaSourceSetBuilder putAll(final Map<String, String> yangResourceNameToContent) {
        this.yangModelMap.putAll(yangResourceNameToContent);
        return this;
    }

    public YangTextSchemaSourceSet build() throws ReactorException, IOException, YangSyntaxErrorException {
        final SchemaContext schemaContext = generateSchemaContext(yangModelMap.build());
        return new YangTextSchemaSourceSetImpl(schemaContext);
    }

    public static YangTextSchemaSourceSet of(final Map<String, String> yangResourceNameToContent)
            throws ReactorException, IOException, YangSyntaxErrorException {
        return new YangTextSchemaSourceSetBuilder().putAll(yangResourceNameToContent).build();
    }

    private static class YangTextSchemaSourceSetImpl implements YangTextSchemaSourceSet {

        private final SchemaContext schemaContext;

        public YangTextSchemaSourceSetImpl(final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
        }

        @Override
        public List<ModuleReference> getModuleReference() {
            return schemaContext.getModules().stream()
                           .map(YangTextSchemaSourceSetImpl::toModuleReference)
                           .collect(Collectors.toList());
        }

        private static ModuleReference toModuleReference(final Module module) {
            return ModuleReference.builder()
                           .namespace(module.getName())
                           .revision(module.getRevision().map(Revision::toString).orElse(null))
                           .build();
        }

        @Override
        public SchemaContext getSchemaContext() {
            return schemaContext;
        }
    }

    /**
     * Parse and validate a string representing a yang model to generate a SchemaContext context.
     *
     * @param yangResourceNameToContent is a {@link Map} collection that contains the name of the model represented
     *                     on yangModelContent as key and the yangModelContent as value.
     * @return the schema context
     */
    private SchemaContext generateSchemaContext(final Map<String, String> yangResourceNameToContent)
            throws IOException, ReactorException, YangSyntaxErrorException {
        final CrossSourceStatementReactor.BuildAction reactor = RFC7950Reactors.defaultReactor().newBuild();
        final List<YangTextSchemaSource> yangTextSchemaSources = forResources(yangResourceNameToContent);
        for (final YangTextSchemaSource yangTextSchemaSource : yangTextSchemaSources) {
            reactor.addSource(YangStatementStreamSource.create(yangTextSchemaSource));
        }
        return reactor.buildEffective();
    }

    private List<YangTextSchemaSource> forResources(final Map<String, String> yangResourceNameToContent) {
        return yangResourceNameToContent.entrySet().stream()
                       .map(entry -> toYangTextSchemaSource(entry.getKey(), entry.getValue()))
                       .collect(Collectors.toList());
    }

    private YangTextSchemaSource toYangTextSchemaSource(final String sourceName, final String source) {
        final Map.Entry<String, String> sourceNameParsed = YangNames.parseFilename(sourceName);
        final RevisionSourceIdentifier revisionSourceIdentifier = RevisionSourceIdentifier
            .create(sourceNameParsed.getKey(), Revision.ofNullable(sourceNameParsed.getValue()));
        return new YangTextSchemaSource(revisionSourceIdentifier) {
            @Override
            protected MoreObjects.ToStringHelper addToStringAttributes(
                    final MoreObjects.ToStringHelper toStringHelper) {
                return toStringHelper;
            }

            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(source.getBytes());
            }
        };
    }
}
