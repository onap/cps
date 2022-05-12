/*
   ============LICENSE_START=======================================================
    Copyright (C) 2021-2022 Nordix Foundation.
    Modifications Copyright (C) 2021 Pantheon.tech
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
    (1002, 'NCMP-Admin'),
    (1003, 'NFP-Operational');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001),
    (2002, 'SCHEMA-SET-002', 1003),
    (2003, 'SCHEMA-SET-003', 1003),
    (2004, 'SCHEMA-SET-004', 1003),
    (2005, 'SCHEMA-SET-005', 1003),
    (2006, 'SCHEMA-SET-006', 1003);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001),
    (3003, 'ANCHOR-003', 1001, 2001),
    (3004, 'ncmp-dmi-registry', 1002, 2001),
    (3005, 'PNFDemo', 1003, 2002),
    (3006, 'PNFDemo2', 1003, 2003),
    (3007, 'PNFDemo3', 1003, 2004),
    (3008, 'PNFDemo4', 1003, 2005);

INSERT INTO YANG_RESOURCE (ID, NAME, CONTENT, CHECKSUM, MODULE_NAME, REVISION) VALUES
    (6001, 'module1@2020-02-02.yang', 'CONTENT-001', 'e8bdda931099310de66532e08c3fafec391db29f55c81927b168f6aa8f81b73b','MODULE-NAME-001','REVISION-001'),
    (6002, 'module2@2020-02-02.yang', 'CONTENT-002', '7e7d48afbe066ed0a890a09081859046d3dde52300dfcdb13be5b20780353a11','MODULE-NAME-002','REVISION-002'),
    (6003, 'module3@2020-02-02.yang', 'CONTENT-003', 'ca20c45fec8547633f05ff8905c48ffa7b02b94ec3ad4ed79922e6ba40779df3','MODULE-NAME-003','REVISION-002'),
    (6004, 'module4@2020-02-02.yang', 'CONTENT-004', 'f6ed09d343562e4d4ae5140f3c6a55df9c53f6da8e30dda8cbd9eaf9cd449be0','MODULE-NAME-004','REVISION-004');

INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) VALUES
    (2002, 6001), (2003, 6002),
    (2003, 6001), (2004, 6001),
    (2005, 6002), (2005, 6003),
    (2004, 6003), (2006, 6004);

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH) VALUES
    (4001, 1001, 3001, null, '/parent-1'),
    (4002, 1001, 3001, null, '/parent-2'),
    (4003, 1001, 3001, null, '/parent-3'),
    (4004, 1001, 3001, 4001, '/parent-1/child-1'),
    (4005, 1001, 3001, 4002, '/parent-2/child-2'),
    (4006, 1001, 3001, 4004, '/parent-1/child-1/grandchild-1');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4101, 1001, 3003, null, '/parent-100', '{"parent-leaf": "parent-leaf value"}'),
    (4102, 1001, 3003, 4101, '/parent-100/child-001', '{"first-child-leaf": "first-child-leaf value"}'),
    (4103, 1001, 3003, 4101, '/parent-100/child-002', '{"second-child-leaf": "second-child-leaf value"}'),
    (4104, 1001, 3003, 4103, '/parent-100/child-002/grand-child', '{"grand-child-leaf": "grand-child-leaf value"}');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (4201, 1001, 3003, null, '/parent-200', '{"leaf-value": "original"}'),
    (4202, 1001, 3003, 4201, '/parent-200/child-201', '{"leaf-value": "original"}'),
    (4203, 1001, 3003, 4202, '/parent-200/child-201/grand-child', '{"leaf-value": "original"}'),
    (4206, 1001, 3003, null, '/parent-201', '{"leaf-value": "original"}'),
    (4207, 1001, 3003, 4206, '/parent-201/child-203', '{}'),
    (4208, 1001, 3003, 4206, '/parent-201/child-204[@key=''A'']', '{"key": "A"}'),
    (4209, 1001, 3003, 4206, '/parent-201/child-204[@key=''B'']', '{"key": "B"}'),
    (4211, 1001, 3003, null, '/parent-202', '{"leaf-value": "original"}'),
    (4212, 1001, 3003, 4211, '/parent-202/child-205[@key=''A'' and @key2=''B'']', '{"key": "A", "key2": "B"}'),
    (4213, 1001, 3003, 4211, '/parent-202/child-206[@key=''A'']', '{"key": "A"}'),
    (4214, 1001, 3003, null, '/parent-203', '{"leaf-value": "original"}'),
    (4215, 1001, 3003, 4214, '/parent-203/child-203', '{}'),
    (4216, 1001, 3003, 4214, '/parent-203/child-204[@key=''A'']', '{"key": "A"}'),
    (4217, 1001, 3003, 4214, '/parent-203/child-204[@key=''B'']', '{"key": "B"}'),
    (4218, 1001, 3003, 4217, '/parent-203/child-204[@key=''B'']/grand-child-204[@key2=''Y'']', '{"key": "B", "key2": "Y"}'),
    (4226, 1001, 3003, null, '/parent-206', '{"leaf-value": "original"}'),
    (4227, 1001, 3003, 4226, '/parent-206/child-206', '{}'),
    (4228, 1001, 3003, 4227, '/parent-206/child-206/grand-child-206', '{}'),
    (4229, 1001, 3003, 4227, '/parent-206/child-206/grand-child-206[@key=''A'']', '{"key": "A"}'),
    (4230, 1001, 3003, 4227, '/parent-206/child-206/grand-child-206[@key=''X'']', '{"key": "X"}'),
    (4231, 1001, 3003, null, '/parent-206[@key=''A'']', '{"key": "A"}'),
    (4232, 1001, 3003, 4231, '/parent-206[@key=''A'']/child-206', '{}'),
    (4233, 1001, 3003, null, '/parent-206[@key=''B'']', '{"key": "B"}');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (5000, 1002, 3004, null, '/dmi-registry/cm-handles[@id=''PNFDemo'']', '{"id": "PNFDemo", "dmi-service-name": "http://172.21.235.14:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5001, 1002, 3004, null, '/dmi-registry/cm-handles[@id=''PNFDemo2'']', '{"id": "PNFDemo2", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5002, 1002, 3004, null, '/dmi-registry/cm-handles[@id=''PNFDemo3'']', '{"id": "PNFDemo3", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5003, 1002, 3004, null, '/dmi-registry/cm-handles[@id=''PNFDemo4'']', '{"id": "PNFDemo4", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5004, 1002, 3004, 5000, '/dmi-registry/cm-handles[@id=''PNFDemo'']/public-properties[@name=''Contact'']', '{"name": "Contact", "value": "newemailforstore@bookstore.com"}'),
    (5005, 1002, 3004, 5001, '/dmi-registry/cm-handles[@id=''PNFDemo2'']/public-properties[@name=''Contact'']', '{"name": "Contact", "value": "newemailforstore@bookstore.com"}'),
    (5006, 1002, 3004, 5002, '/dmi-registry/cm-handles[@id=''PNFDemo3'']/public-properties[@name=''Contact'']', '{"name": "Contact3", "value": "PNF3@bookstore.com"}'),
    (5007, 1002, 3004, 5003, '/dmi-registry/cm-handles[@id=''PNFDemo4'']/public-properties[@name=''Contact'']', '{"name": "Contact", "value": "newemailforstore@bookstore.com"}'),
    (5008, 1002, 3004, 5004, '/dmi-registry/cm-handles[@id=''PNFDemo4'']/public-properties[@name=''Contact2'']', '{"name": "Contact2", "value": "newemailforstore2@bookstore.com"}');
