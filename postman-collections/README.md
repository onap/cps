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
To import the CPS collections and environment simply create a "Workspace", click "import", unzip "cps-postman-collections.zip" and drop the "cps-postman-collections" folder into the import pop-up.

Don't forget to set the environment by clicking the dropdown that says "No Environment" and selecting "CPS Environment". This will provide the necessary variables such as "CPS_HOST" and "CPS_PORT" to allow the requests to be run.
## Running the collections
To run the requests in the collections simply select the request and click send. "Create Schema Set" in "CPS-CORE" requires a file to send the request. An example file is provided: "bookstore-model.yang"
