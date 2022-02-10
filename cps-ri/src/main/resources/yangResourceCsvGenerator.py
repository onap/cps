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
import datetime
import hashlib
import sys
import re

yang_source = ''
checksum = ''
regexForModuleName = '(?<=module)(.*)(?={)'
regexForRevision = '(?<=revision)(.*)(?={)'

def main():
    for yang_source in sys.argv[1:]:
        checksum = hashlib.sha256(str(yang_source).encode()).hexdigest()

        with open('changelog/db/changes/data/yang-models/' + yang_source + '.yang', 'r') as content:
            dmiRegistry = content.read()

        try:
            latestRevision = re.search(regexForRevision, dmiRegistry).group(0).replace('\"','').strip()
        except:
            print("ERROR IN in yangResourceCsvGenerator.py: Unable to find revision for " + yang_source + '.yang')

        try:
            module_name = re.search(regexForModuleName, dmiRegistry).group(0).strip()
        except:
            print("ERROR IN in yangResourceCsvGenerator.py: Unable to find module name for " + yang_source + '.yang')

        #If true, file was created after module name and revision were added to yang-resources table
        writeWithModuleNameAndRevision = yang_source != 'dmi-registry@2021-12-13'

        print(writeWithModuleNameAndRevision)

        try:
            # open the file in the write mode
            with open('changelog/db/changes/data/dmi/generated-csv/generated_yang_resource_' + yang_source + '.csv', 'w', newline='') \
                    as file:
                writer = csv.writer(file, delimiter='|')
                if(writeWithModuleNameAndRevision):
                    writer.writerow(["name", "content", "checksum", "module_name", "revision"])
                    writer.writerow([yang_source + '.yang', dmiRegistry, checksum, module_name, latestRevision])
                else:
                    writer.writerow(["name", "content", "checksum"])
                    writer.writerow([yang_source + '.yang', dmiRegistry, checksum])
        except:
            print("ERROR IN in yangResourceCsvGenerator.py: Unable to write to changelog/db/changes/data/dmi/generated-csv/generated_yang_resource_" + yang_source + ".csv")


main()