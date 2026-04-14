<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
  ================================================================================
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  SPDX-License-Identifier: Apache-2.0
  ============LICENSE_END=========================================================
-->



# provmns-api

## Accessing provmns-api in IntelliJ

If IntelliJ cannot resolve classes from `provmns-api`, manually add the JAR to the project modules:

1. Navigate to **File > Project Structure > Project Settings > Libraries**
2. Scroll down to `Maven: org.onap.cps:provmns-api:18.6.0`
3. Right-click and select **Add to Modules**
4. Add to both `cps-ncmp-rest` and `cps-ncmp-service` (use CTRL+click to select multiple)

> **Note:** A Maven reload (not a clean install) will reset these settings. Repeat the steps above if that happens.
