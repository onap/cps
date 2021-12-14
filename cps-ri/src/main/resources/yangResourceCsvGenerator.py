#  ============LICENSE_START=======================================================
#  Copyright (C) 2022 Nordix Foundation
#  ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  ============LICENSE_END=========================================================


import csv
import hashlib
import sys

yang_source = ''
checksum = ''

for yang_source in sys.argv[1:]:
    checksum = hashlib.sha256(str(yang_source).encode()).hexdigest()

with open('changelog/db/changes/data/yang-models/' + yang_source + '.yang') as content:
    dmiRegistry = content.read()

# open the file in the write mode
with open('changelog/db/changes/data/dmi/generated-csv/generated_yang_resource_' + yang_source + '.csv', 'w', newline='') \
        as file:
    writer = csv.writer(file, delimiter='|')
    writer.writerow(["name", "content", "checksum"])
    writer.writerow([yang_source + '.yang', dmiRegistry, checksum])
