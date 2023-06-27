/*
   ============LICENSE_START=======================================================
    Copyright (C) 2023 Nordix Foundation.
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

-- replace '' with \' and " with ""
-- SQL strings use single-quote like 'example', where quotes need to be escaped, e.g. 'it''s good'.
-- Using raw quotes like $$example$$, we can avoid escaping, e.g. $$it's good$$
UPDATE fragment SET xpath = replace(replace(xpath, $$''$$, $$\'$$), '"', '""');