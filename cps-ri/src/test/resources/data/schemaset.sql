/*
   ============LICENSE_START=======================================================
    Copyright (C) 2020-2021 Pantheon.tech
    Modifications Copyright (C) 2020 Nordix Foundation.
    Modifications Copyright (C) 2020-2021 Bell Canada.
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
    (1001, 'DATASPACE-001'), (1002, 'DATASPACE-002');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001),
    (2002, 'SCHEMA-SET-002', 1001),
    (2100, 'SCHEMA-SET-100', 1001), -- for removal, not referenced by anchors
    (2101, 'SCHEMA-SET-101', 1001), -- for removal, having anchor and data associated
    (2003, 'SCHEMA-SET-003', 1002),
    (2004, 'SCHEMA-SET-004', 1002);

INSERT INTO YANG_RESOURCE (ID, NAME, CONTENT, CHECKSUM, MODULE_NAME, REVISION) VALUES
    (3001, 'module1@2020-02-02.yang', 'CONTENT-001', 'e8bdda931099310de66532e08c3fafec391db29f55c81927b168f6aa8f81b73b',null,null),
    (3002, 'module2@2020-02-02.yang', 'CONTENT-002', '7e7d48afbe066ed0a890a09081859046d3dde52300dfcdb13be5b20780353a11','MODULE-NAME-002','REVISION-002'),
    (3003, 'module3@2020-02-02.yang', 'CONTENT-003', 'ca20c45fec8547633f05ff8905c48ffa7b02b94ec3ad4ed79922e6ba40779df3','MODULE-NAME-003','REVISION-002'),
    (3004, 'module4@2020-02-02.yang', 'CONTENT-004', 'f6ed09d343562e4d4ae5140f3c6a55df9c53f6da8e30dda8cbd9eaf9cd449be0','MODULE-NAME-004','REVISION-004'),
    (3100, 'orphan@2020-02-02.yang', 'ORPHAN', 'checksum',null,null), -- for auto-removal as orphan
    (3005, 'module5@2020-02-02.yang', 'CONTENT-005', 'checksum-005','MODULE-NAME-005','REVISION-002'),
    (3006, 'module6@2020-02-02.yang', 'CONTENT-006', 'checksum-006','MODULE-NAME-006','REVISION-006');

INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) VALUES
    (2001, 3001), (2001, 3002),
    (2002, 3003), (2002, 3004),
    (2100, 3003), (2100, 3100), -- orphan removal case
    (2101, 3003), (2101, 3004),
    (2003, 3005), (2004, 3006);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES -- anchors for removal
    (6001, 'ANCHOR1', 1001, 2101),
    (6002, 'ANCHOR2', 1001, 2101);

INSERT INTO FRAGMENT (ID, XPATH, ANCHOR_ID, DATASPACE_ID) VALUES
    (7001, '/XPATH', 6001, 1001);