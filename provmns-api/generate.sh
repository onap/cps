#!/bin/bash
# ============LICENSE_START=======================================================
#  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#
# Regenerates the provmns-api JAR from the 3GPP TS28532 ProvMnS spec and
# publishes it to local-repo. Run this script from the repo root when the
# upstream spec version changes.
#
# Prerequisites:
#   - Java 17 (ensure JAVA_HOME points to Java 17 before running)
#   - Network access to forge.3gpp.org
#
# Usage:
#   ./provmns-api/generate.sh

set -e

# Derive JAVA_HOME from the active java command so Maven uses the same JVM
export JAVA_HOME=$(java -XshowSettings:property -version 2>&1 | awk '/java.home/{print $3}')

if ! java -version 2>&1 | grep -q 'version "17'; then
    echo "ERROR: Java 17 is required. Set JAVA_HOME to a Java 17 installation."
    java -version 2>&1
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LIB_VERSION=$(mvn help:evaluate -Dexpression=provmns.lib.version -q -DforceStdout -f "$SCRIPT_DIR/pom.xml")

echo ">>> Step 1: Generating sources from 3GPP spec and compiling to local-repo (version: $LIB_VERSION)..."
mvn generate-sources -Pgenerate-provmns-api -pl provmns-api -f "$REPO_ROOT/pom.xml"

echo ">>> Step 2: Building and installing merged JAR..."
mvn install -pl provmns-api -f "$REPO_ROOT/pom.xml"

echo ""
echo ">>> Done. JAR file created at:"
echo "    provmns-api/local-repo/org/onap/cps/provmns-api/${LIB_VERSION}/provmns-api-${LIB_VERSION}.jar"
