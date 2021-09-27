/*
   ============LICENSE_START=======================================================
    Copyright (C) 2021 Nordix Foundation.
    Modifications Copyright (C) 2021 Pantheon.tech
    Modifications Copyright (C) 2021 Bell Canada.
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
    (1001, 'DATASPACE-001');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'SCHEMA-SET-001', 1001);

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (3001, 'ANCHOR-001', 1001, 2001),
    (3003, 'ANCHOR-003', 1001, 2001);

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
    (4204, 1001, 3003, 4201, '/parent-200/child-202', '{"common-leaf-name": "common-leaf value", "common-leaf-name-int" : 5}'),
    (4205, 1001, 3003, 4204, '/parent-200/child-202/grand-child-202[@key="D"]', '{"common-leaf-name": "common-leaf value", "common-leaf-name-int" : 5}'),
    (4206, 1001, 3003, null, '/parent-201', '{"leaf-value": "original"}'),
    (4207, 1001, 3003, 4206, '/parent-201/child-203', '{}'),
    (4208, 1001, 3003, 4206, '/parent-201/child-204[@key="A"]', '{"key": "A"}'),
    (4209, 1001, 3003, 4206, '/parent-201/child-204[@key="X"]', '{"key": "X"}'),
    (4211, 1001, 3003, null, '/parent-202', '{"leaf-value": "original"}'),
    (4212, 1001, 3003, 4211, '/parent-202/child-205[@key="A" and @key2="B"]', '{"key": "A", "key2": "B"}'),
    (4213, 1001, 3003, 4211, '/parent-202/child-206[@key="A"]', '{"key": "A"}'),
    (4214, 1001, 3003, null, '/parent-203', '{"leaf-value": "original"}'),
    (4215, 1001, 3003, 4214, '/parent-203/child-203', '{}'),
    (4216, 1001, 3003, 4214, '/parent-203/child-204[@key="A"]', '{"key": "A"}'),
    (4217, 1001, 3003, 4214, '/parent-203/child-204[@key="X"]', '{"key": "X"}'),
    (4218, 1001, 3003, 4217, '/parent-203/child-204[@key="X"]/grand-child-204[@key2="Y"]', '{"key": "X", "key2": "Y"}'),
    (4219, 1001, 3003, null, '/parent-204[@key="L"]', '{"key": "L"}'),
    (4220, 1001, 3003, 4219, '/parent-204[@key="L"]/child-210[@key="M"]', '{"key": "M"}'),
    (4221, 1001, 3003, null, '/parent-205', '{"leaf-value": "original"}'),
    (4222, 1001, 3003, 4221, '/parent-205/child-205', '{}'),
    (4223, 1001, 3003, 4221, '/parent-205/child-205[@key="X"]', '{"key": "X"}'),
    (4224, 1001, 3003, 4223, '/parent-205/child-205[@key="X"]/grand-child-206[@key="Y"]', '{"key": "Y", "key2": "Z"}'),
    (4225, 1001, 3003, 4223, '/parent-205/child-205[@key="X"]/grand-child-206[@key="Y" and @key2="Z"]', '{"key": "Y", "key2": "Z"}');