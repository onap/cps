<!--
  ============LICENSE_START=======================================================
     Copyright (C) 2024 Nordix Foundation.
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
-->

# Importing and running the CPS postman collections
## Importing the CPS collections
To import the CPS collections and environment:
1. Create a "Workspace" (or if you have one already you can use that)
2. Click "Import" (or click "File" then "Import")
3. Drag and drop the "postman-collection" folder into the import pop-up
4. The collections and environment should now be imported
5. Set the current environment to "CPS Environment" (usually at the top right. Default is "No Environment") This will provide the necessary variables such as "CPS_HOST" and "CPS_PORT" to allow the requests to be run

## Running the collections
To run the requests in the collections simply select the request and click send. "Create Schema Set" in "CPS-CORE" requires a file to send the request. An example file is provided: "bookstore-model.yang"
