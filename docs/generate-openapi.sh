#!/bin/bash
#  ============LICENSE_START=======================================================
#  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
#  ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  ============LICENSE_END=========================================================

# Generates merged OpenAPI YAML files from the multi-file source specs.
# Used by the ReadTheDocs build (see .readthedocs.yaml) so these files
# do not need to be committed to the repository.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OPENAPI_GENERATOR_VERSION="7.12.0"
OPENAPI_GENERATOR_JAR="$SCRIPT_DIR/openapi-generator-cli.jar"
OPENAPI_GENERATOR_URL="https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/$OPENAPI_GENERATOR_VERSION/openapi-generator-cli-$OPENAPI_GENERATOR_VERSION.jar"

# Download openapi-generator-cli if not already present
if [ ! -f "$OPENAPI_GENERATOR_JAR" ]; then
    echo "Downloading openapi-generator-cli $OPENAPI_GENERATOR_VERSION..."
    curl -sL "$OPENAPI_GENERATOR_URL" -o "$OPENAPI_GENERATOR_JAR"
fi

generate() {
    local input_spec="$1"
    local output_dir="$2"
    local output_file="$3"

    mkdir -p "$output_dir"
    echo "Generating $output_dir/$output_file from $input_spec"
    java -jar "$OPENAPI_GENERATOR_JAR" generate \
        -i "$input_spec" \
        -g openapi-yaml \
        -o "$output_dir" \
        --additional-properties=outputFile="$output_file" \
        > /dev/null 2>&1

    # Clean up extra files the generator creates (README.md, .openapi-generator/)
    rm -rf "$output_dir/.openapi-generator" "$output_dir/README.md" "$output_dir/.openapi-generator-ignore"
}

# CPS Core
generate \
    "$PROJECT_ROOT/cps-rest/docs/openapi/openapi.yml" \
    "$PROJECT_ROOT/docs/api/swagger/cps" \
    "openapi.yaml"

# NCMP
generate \
    "$PROJECT_ROOT/cps-ncmp-rest/docs/openapi/openapi.yml" \
    "$PROJECT_ROOT/docs/api/swagger/ncmp" \
    "openapi.yaml"

# NCMP Inventory
generate \
    "$PROJECT_ROOT/cps-ncmp-rest/docs/openapi/openapi-inventory.yml" \
    "$PROJECT_ROOT/docs/api/swagger/ncmp" \
    "openapi-inventory.yaml"

echo "OpenAPI documentation generated successfully."
