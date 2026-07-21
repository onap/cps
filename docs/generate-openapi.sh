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
#
# Safe to run concurrently: an flock guard serialises invocations so that
# parallel Sphinx builds (e.g. the tox "docs" and "docs-linkcheck"
# environments) cannot race on the shared generator JAR or the shared
# output directories. The JAR download and each generated spec are written
# to a temporary location and moved into place atomically.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OPENAPI_GENERATOR_VERSION="7.12.0"
OPENAPI_GENERATOR_JAR="$SCRIPT_DIR/openapi-generator-cli.jar"
OPENAPI_GENERATOR_URL="https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/$OPENAPI_GENERATOR_VERSION/openapi-generator-cli-$OPENAPI_GENERATOR_VERSION.jar"

CPS_OUTPUT_DIR="$PROJECT_ROOT/docs/api/swagger/cps"
NCMP_OUTPUT_DIR="$PROJECT_ROOT/docs/api/swagger/ncmp"

# The complete set of generated files; used to short-circuit when a
# concurrent invocation has already produced everything.
OUTPUT_FILES=(
    "$CPS_OUTPUT_DIR/openapi.yaml"
    "$NCMP_OUTPUT_DIR/openapi.yaml"
    "$NCMP_OUTPUT_DIR/openapi-inventory.yaml"
)

# Working directory for atomic writes; kept on the same filesystem as the
# destinations (under docs/) so that "mv" into place is atomic. Removed on
# exit, including on error.
WORK_DIR=""
cleanup() {
    if [ -n "$WORK_DIR" ] && [ -d "$WORK_DIR" ]; then
        rm -rf "$WORK_DIR"
    fi
}
trap cleanup EXIT

all_outputs_present() {
    local file
    for file in "${OUTPUT_FILES[@]}"; do
        [ -f "$file" ] || return 1
    done
    return 0
}

download_generator() {
    if [ -f "$OPENAPI_GENERATOR_JAR" ]; then
        return 0
    fi
    echo "Downloading openapi-generator-cli $OPENAPI_GENERATOR_VERSION..."
    local tmp_jar="$WORK_DIR/openapi-generator-cli.jar"
    curl -fsSL "$OPENAPI_GENERATOR_URL" -o "$tmp_jar"
    mv "$tmp_jar" "$OPENAPI_GENERATOR_JAR"
}

generate() {
    local input_spec="$1"
    local output_dir="$2"
    local output_file="$3"

    local tmp_out
    tmp_out="$(mktemp -d "$WORK_DIR/gen.XXXXXX")"

    echo "Generating $output_dir/$output_file from $input_spec"
    java -jar "$OPENAPI_GENERATOR_JAR" generate \
        -i "$input_spec" \
        -g openapi-yaml \
        -o "$tmp_out" \
        --additional-properties=outputFile="$output_file" \
        > /dev/null 2>&1

    mkdir -p "$output_dir"
    # Move only the generated spec into place; the generator's scratch
    # files (.openapi-generator/, README.md, etc.) are discarded with the
    # temporary directory.
    mv "$tmp_out/$output_file" "$output_dir/$output_file"
    rm -rf "$tmp_out"
}

generate_all() {
    # Short-circuit if a concurrent invocation produced everything while we
    # were waiting for the lock.
    if all_outputs_present; then
        echo "OpenAPI documentation already present; nothing to do."
        return 0
    fi

    WORK_DIR="$(mktemp -d "$SCRIPT_DIR/.openapi-gen.XXXXXX")"

    download_generator

    # CPS Core
    generate \
        "$PROJECT_ROOT/cps-rest/docs/openapi/openapi.yml" \
        "$CPS_OUTPUT_DIR" \
        "openapi.yaml"

    # NCMP
    generate \
        "$PROJECT_ROOT/cps-ncmp-rest/docs/openapi/openapi.yml" \
        "$NCMP_OUTPUT_DIR" \
        "openapi.yaml"

    # NCMP Inventory
    generate \
        "$PROJECT_ROOT/cps-ncmp-rest/docs/openapi/openapi-inventory.yml" \
        "$NCMP_OUTPUT_DIR" \
        "openapi-inventory.yaml"

    echo "OpenAPI documentation generated successfully."
}

# Serialise concurrent invocations. The lock is released automatically when
# the file descriptor closes on exit. When flock is unavailable (e.g. some
# local development environments) fall back to running without the guard.
LOCK_FILE="$SCRIPT_DIR/.openapi-generator.lock"
if command -v flock > /dev/null 2>&1; then
    exec 9> "$LOCK_FILE"
    flock 9
    generate_all
else
    generate_all
fi
