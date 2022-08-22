/*
   ============LICENSE_START=======================================================
    Copyright (C) 2020-2021 Pantheon.tech
    Modifications Copyright (C) 2020,2022 Nordix Foundation.
    Modifications Copyright (C) 2020 Bell Canada.
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
*/

DELETE FROM FRAGMENT;
DELETE FROM ANCHOR;
DELETE FROM DATASPACE;
DELETE FROM YANG_RESOURCE
-- following tables are cleared by CASCADE constraint: SCHEMA_SET, SCHEMA_SET_YANG_RESOURCES

