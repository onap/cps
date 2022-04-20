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
    (1002, 'NCMP-Admin');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001),
    (3003, 'ANCHOR-003', 1001, 2001),
    (3004, 'ncmp-dmi-registry', 1002, 2001);

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
    (4208, 1001, 3003, 4206, '/parent-201/child-204[@key="A"]', '{"key": "A"}'),
    (4209, 1001, 3003, 4206, '/parent-201/child-204[@key="B"]', '{"key": "B"}'),
    (4211, 1001, 3003, null, '/parent-202', '{"leaf-value": "original"}'),
    (4212, 1001, 3003, 4211, '/parent-202/child-205[@key="A" and @key2="B"]', '{"key": "A", "key2": "B"}'),
    (4213, 1001, 3003, 4211, '/parent-202/child-206[@key="A"]', '{"key": "A"}'),
    (4214, 1001, 3003, null, '/parent-203', '{"leaf-value": "original"}'),
    (4215, 1001, 3003, 4214, '/parent-203/child-203', '{}'),
    (4216, 1001, 3003, 4214, '/parent-203/child-204[@key="A"]', '{"key": "A"}'),
    (4217, 1001, 3003, 4214, '/parent-203/child-204[@key="B"]', '{"key": "B"}'),
    (4218, 1001, 3003, 4217, '/parent-203/child-204[@key="B"]/grand-child-204[@key2="Y"]', '{"key": "B", "key2": "Y"}'),
    (4226, 1001, 3003, null, '/parent-206', '{"leaf-value": "original"}'),
    (4227, 1001, 3003, 4226, '/parent-206/child-206', '{}'),
    (4228, 1001, 3003, 4227, '/parent-206/child-206/grand-child-206', '{}'),
    (4229, 1001, 3003, 4227, '/parent-206/child-206/grand-child-206[@key="A"]', '{"key": "A"}'),
    (4230, 1001, 3003, 4227, '/parent-206/child-206/grand-child-206[@key="X"]', '{"key": "X"}'),
    (4231, 1001, 3003, null, '/parent-206[@key="A"]', '{"key": "A"}'),
    (4232, 1001, 3003, 4231, '/parent-206[@key="A"]/child-206', '{}'),
    (4233, 1001, 3003, null, '/parent-206[@key="B"]', '{"key": "B"}');

INSERT INTO FRAGMENT (ID, DATASPACE_ID, ANCHOR_ID, PARENT_ID, XPATH, ATTRIBUTES) VALUES
    (5000, 1002, 3004, null, '/dmi-registry/cm-handles[@id="PNFDemo"]', '{"id": "PNFDemo", "dmi-service-name": "http://172.21.235.14:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5001, 1002, 3004, null, '/dmi-registry/cm-handles[@id="PNFDemo2"]', '{"id": "PNFDemo2", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5002, 1002, 3004, null, '/dmi-registry/cm-handles[@id="PNFDemo3"]', '{"id": "PNFDemo3", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5003, 1002, 3004, null, '/dmi-registry/cm-handles[@id="PNFDemo4"]', '{"id": "PNFDemo4", "state": "READY", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5004, 1002, 3004, null, '/dmi-registry/cm-handles[@id="PNFDemo5"]', '{"id": "PNFDemo5", "state": "ADVISED", "dmi-service-name": "http://172.26.46.68:8783", "dmi-data-service-name": "", "dmi-model-service-name": ""}'),
    (5005, 1002, 3004, 5000, '/dmi-registry/cm-handles[@id="PNFDemo"]/public-properties[@name="Contact"]', '{"name": "Contact", "value": "newemailforstore@bookstore.com"}'),
    (5006, 1002, 3004, 5001, '/dmi-registry/cm-handles[@id="PNFDemo2"]/public-properties[@name="Contact"]', '{"name": "Contact", "value": "newemailforstore@bookstore.com"}'),
    (5007, 1002, 3004, 5002, '/dmi-registry/cm-handles[@id="PNFDemo3"]/public-properties[@name="Contact"]', '{"name": "Contact3", "value": "PNF3@bookstore.com"}'),
    (5008, 1002, 3004, 5003, '/dmi-registry/cm-handles[@id="PNFDemo4"]/public-properties[@name="Contact"]', '{"name": "Contact", "value": "newemailforstore@bookstore.com"}'),
    (5009, 1002, 3004, 5004, '/dmi-registry/cm-handles[@id="PNFDemo4"]/public-properties[@name="Contact2"]', '{"name": "Contact2", "value": "newemailforstore2@bookstore.com"}');