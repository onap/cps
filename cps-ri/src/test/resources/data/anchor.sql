/*
   ============LICENSE_START=======================================================
    Copyright (C) 2020 Pantheon.tech
    Modifications Copyright (C) 2020-2022 Nordix Foundation.
    Modifications Copyright (C) 2021-2022 Bell Canada.
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

INSERT INTO DATASPACE (ID, NAME) VALUES
    (1001, 'DATASPACE-001'),
    (1002, 'DATASPACE-002-NO-DATA'),
    (1003, 'DATASPACE-003'),
    (1004, 'NCMP-Admin');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001),
    (2002, 'SCHEMA-SET-002', 1001),
    (2003, 'SCHEMA-SET-002-NO-ANCHORS', 1003);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001),
    (3002, 'ANCHOR-002', 1001, 2002),
    (3003, 'ncmp-dmi-registry', 1004, 2001);

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4001, 1001, 3001, null, '/xpath', '{}'),
    (4002, 1004, 3003, null, '/dmi-registry/cm-handles[@id=''PNFDemo'']', '{"id": "PNFDemo", "state": "READY", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}');
