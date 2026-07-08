---
name: copyright-manager
description: Rules for updating copyright headers whenever a file is modified.
---

# License Header Rules for OpenInfra Foundation Europe Team
## When to Update License Headers
Update the license header **every time you modify a or create a Java or Groovy file**.
## Rules
### 1. **New files**
Add copyright with current year only:
```java
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
```
### 2. **If file has OpenInfra copyright already**
Update the year range to current year:
```java
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
```
### 3. **If file has Nordix copyright**
Replace "Nordix" with "OpenInfra" and update year:
```java
// Change this:
Copyright (C) 2021-2024 Nordix Foundation
// To this:
Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
```
### 4. **If file has OTHER organization copyright**
Add a "Modifications" line below the original:
```java
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
```
### 5. **If modifications line exists**
Update the year range:
```java
// Change this:
Modifications Copyright (C) 2022-2024 OpenInfra Foundation Europe. All rights reserved.
// To this:
Modifications Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
```
## Standard Template
```java
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) YYYY[-YYYY] OpenInfra Foundation Europe. All rights reserved.
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
```
## Quick Reference
| Scenario | Action |
|----------|--------|
| **New file** | **Add copyright with current year only** |
| OpenInfra copyright exists | Update end year |
| Nordix copyright exists | Replace with OpenInfra, update year |
| Other org copyright exists | Add "Modifications Copyright (C) YYYY OpenInfra..." |
| Modifications line exists | Update end year |
