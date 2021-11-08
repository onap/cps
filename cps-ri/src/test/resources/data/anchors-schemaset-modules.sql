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
    (1001, 'dataspace-1'), (1002, 'dataspace-2');

INSERT INTO SCHEMA_SET (ID, NAME, DATASPACE_ID) VALUES
    (2001, 'schema-set-1', 1001),
    (2002, 'schema-set-2', 1001),
    (2003, 'schema-set-3', 1001),
    (2004, 'schema-set-4', 1002);

INSERT INTO YANG_RESOURCE (ID, NAME, CONTENT, CHECKSUM, MODULE_NAME, REVISION) VALUES
    (3001, 'module1@revA.yang', 'some-content', 'checksum1','module-name-1','revA'),
    (3002, 'module2@revA.yang', 'some-content', 'checksum2','module-name-2','revA'),
    (3003, 'module2@revB.yang', 'some-content', 'checksum3','module-name-2','revB'),
    (3004, 'module3@revA.yang', 'some-content', 'checksum4','module-name-3','revA');

INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) VALUES
    (2001, 3001), --schema-set-1(anchor-1) has modules module1@revA, module2@revA
    (2001, 3002),
    (2002, 3001), --schema-set-2(anchor-2) has modules module1@revA, module2@revB
    (2002, 3003),
    (2003, 3002), --schema-set-3(anchor-3) has modules module2@revA, module2@revB
    (2003, 3003),
    (2004, 3001); --schema-set-4(anchor-4) has module module1@revA but in other dataspace

INSERT INTO ANCHOR (ID, NAME, DATASPACE_ID, SCHEMA_SET_ID) VALUES
    (6001, 'anchor-1', 1001, 2001),
    (6002, 'anchor-2', 1001, 2002),
    (6003, 'anchor-3', 1001, 2003),
    (6005, 'anchor-4', 1002, 2004);
