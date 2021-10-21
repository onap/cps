/*
   ============LICENSE_START=======================================================
    Copyright (C) 2021 Nordix Foundation.
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
    (2002, 'SCHEMA-SET-002', 1002),
    (2003, 'SCHEMA-SET-003', 1001),
    (2004, 'SCHEMA-SET-004', 1001);

INSERT INTO YANG_RESOURCE (ID, NAME, CONTENT, CHECKSUM, MODULE_NAME, REVISION) VALUES
    (3001, 'module1@2020-02-02.yang', 'CONTENT-001', 'e8bdda931099310de66532e08c3fafec391db29f55c81927b168f6aa8f81b73b','MODULE-NAME-001',null),
    (3002, 'module2@2020-02-02.yang', 'CONTENT-002', '7e7d48afbe066ed0a890a09081859046d3dde52300dfcdb13be5b20780353a11','MODULE-NAME-002','REVISION-002'),
    (3003, 'module3@2020-02-02.yang', 'CONTENT-003', '7e7d48afbe066ed0a890a09081859046d3dde52300dfcdb13be5b20780353a12','MODULE-NAME-003','REVISION-003'),
    (3004, 'module4@2020-02-02.yang', 'CONTENT-004', '7e7d48afbe066ed0a890a09081859046d3dde52300dfcdb13be5b20780353a13','MODULE-NAME-004','REVISION-004'),
    (3005, 'module5@2020-03-02.yang', 'CONTENT-005', '7e7d48afbe066ed0a890a09081859046d3dde52300dfcdb13be5b20780353a14','MODULE-NAME-002','REVISION-003');

INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) VALUES
    (2001, 3001), (2002, 3002),
    (2001, 3003), (2002, 3003),
    (2003, 3004), (2004, 3005);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (6001, 'ANCHOR1', 1001, 2001),
    (6002, 'ANCHOR2', 1001, 2002),
    (6003, 'ANCHOR3', 1001, 2004);
