/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.cps.yang;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.annotation.Timed;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.onap.cps.api.exceptions.ModelValidationException;
import org.onap.cps.api.model.ModuleReference;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.RevisionSourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.xpath.impl.di.DefaultXPathParserFactory;

@NoArgsConstructor
public final class YangTextSchemaSourceSetBuilder {

    private static final Pattern RFC6020_RECOMMENDED_FILENAME_PATTERN =
        Pattern.compile("([\\w-]+)@(\\d{4}-\\d{2}-\\d{2})(?:\\.yang)?", Pattern.CASE_INSENSITIVE);

    private final ImmutableMap.Builder<String, String> yangModelMap = new ImmutableMap.Builder<>();

    private static final YangParserFactory YANG_PARSER_FACTORY =
            new DefaultYangParserFactory(new DefaultXPathParserFactory());

    /**
     * Add Yang resource context.
     *
     * @param yangResourceNameToContent the resource content
     * @return this builder
     */
    public YangTextSchemaSourceSetBuilder putAll(final Map<String, String> yangResourceNameToContent) {
        this.yangModelMap.putAll(yangResourceNameToContent);
        return this;
    }

    /**
     * Build a YangTextSchemaSourceSet.
     *
     * @return the YangTextSchemaSourceSet
     */
    public YangTextSchemaSourceSet build() {
        final var schemaContext = generateSchemaContext(yangModelMap.build());
        return new YangTextSchemaSourceSetImpl(schemaContext);
    }

    /**
     * Add yangResourceNameToContent and build a YangTextSchemaSourceSet.
     *
     * @param yangResourceNameToContent the resource content
     * @return the YangTextSchemaSourceSet
     */

    @Timed(value = "cps.yang.schemasourceset.build", description = "Time taken to build a ODL yang Model")
    public static YangTextSchemaSourceSet of(final Map<String, String> yangResourceNameToContent) {
        return new YangTextSchemaSourceSetBuilder().putAll(yangResourceNameToContent).build();
    }

    /**
     * Validates if SchemaContext can be successfully built from given yang resources.
     *
     * @param yangResourceNameToContent the yang resources as map where key is name and value is content
     * @throws ModelValidationException if validation fails
     */
    public static void validate(final Map<String, String> yangResourceNameToContent) {
        generateSchemaContext(yangResourceNameToContent);
    }

    private static class YangTextSchemaSourceSetImpl implements YangTextSchemaSourceSet {

        private final SchemaContext schemaContext;

        private YangTextSchemaSourceSetImpl(final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
        }

        @Override
        public List<ModuleReference> getModuleReferences() {
            return schemaContext.getModules().stream()
                .map(YangTextSchemaSourceSetImpl::toModuleReference)
                .collect(Collectors.toList());
        }

        private static ModuleReference toModuleReference(final Module module) {
            return ModuleReference.builder()
                .moduleName(module.getName())
                .namespace(module.getQNameModule().getNamespace().toString())
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
     *                                  on yangModelContent as key and the yangModelContent as value.
     * @return the schema context
     */
    private static SchemaContext generateSchemaContext(final Map<String, String> yangResourceNameToContent) {
        final YangParser yangParser = YANG_PARSER_FACTORY.createParser();
        for (final YangTextSchemaSource yangTextSchemaSource : forResources(yangResourceNameToContent)) {
            final String resourceName = yangTextSchemaSource.getIdentifier().getName();
            try {
                yangParser.addSource(yangTextSchemaSource);
            } catch (final Exception exception) {
                throw new ModelValidationException("Yang resource processing exception.",
                    String.format("Could not process resource %s:%n%s", resourceName, exception.getMessage()),
                    exception);
            }
        }
        try {
            return yangParser.buildEffectiveModel();
        } catch (final YangParserException yangParserException) {
            final List<String> resourceNames = yangResourceNameToContent.keySet().stream().collect(Collectors.toList());
            Collections.sort(resourceNames);
            throw new ModelValidationException("Invalid schema set.",
                String.format("Effective schema context build failed for resources %s.", resourceNames),
                yangParserException);
        }
    }

    private static List<YangTextSchemaSource> forResources(final Map<String, String> yangResourceNameToContent) {
        return yangResourceNameToContent.entrySet().stream()
            .map(entry -> toYangTextSchemaSource(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    private static YangTextSchemaSource toYangTextSchemaSource(final String sourceName, final String source) {
        final var revisionSourceIdentifier =
            createIdentifierFromSourceName(checkNotNull(sourceName));

        return new YangTextSchemaSource(revisionSourceIdentifier) {
            @Override
            public Optional<String> getSymbolicName() {
                return Optional.empty();
            }

            @Override
            protected MoreObjects.ToStringHelper addToStringAttributes(
                final MoreObjects.ToStringHelper toStringHelper) {
                return toStringHelper;
            }

            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private static RevisionSourceIdentifier createIdentifierFromSourceName(final String sourceName) {
        final var matcher = RFC6020_RECOMMENDED_FILENAME_PATTERN.matcher(sourceName);
        if (matcher.matches()) {
            return RevisionSourceIdentifier.create(matcher.group(1), Revision.of(matcher.group(2)));
        }
        return RevisionSourceIdentifier.create(sourceName);
    }
}
