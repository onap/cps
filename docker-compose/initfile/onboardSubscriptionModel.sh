#!/bin/bash
#
# ============LICENSE_START=======================================================
#   Copyright (C) 2023 Nordix Foundation.
# ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================


echo "Getting ready to upload model for subscription events ..."

createSchemaSetBasePath="/cps/api/v2/dataspaces/NCMP-Admin/schema-sets?schema-set-name=subscriptions"
createAnchorBasePath="/cps/api/v2/dataspaces/NCMP-Admin/anchors?schema-set-name=subscriptions&anchor-name=AVC-subscriptions"

RETRIES=0
while :
do
  status="UP"
  cpsAndNcmpAsHostAddress=$(ping -c1 cps-and-ncmp | sed -nE 's/^PING[^(]+\(([^)]+)\).*/\1/p')

  if  curl -X 'GET' 'http://'"$cpsAndNcmpAsHostAddress"':8081/manage/health/readiness' | grep -q "$status"
  then
        echo "Checking that NCMP dataspace exists ..."
        ncmpDataspaceExists=$(curl --write-out %{http_code} --silent --output /dev/null -X 'GET' 'http://'"$cpsAndNcmpAsHostAddress"':8080/cps/api/v2/admin/dataspaces/NCMP-Admin' -H 'accept: */*' --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')

        echo "NCMP dataspace exist:  $ncmpDataspaceExists"

        if [ "$ncmpDataspaceExists" == 200 ]
        then
          echo "Uploading model ..."
          create_schema_set_status_code=$(curl --write-out %{http_code} --silent --output /dev/null -X 'POST' 'http://'"$cpsAndNcmpAsHostAddress"':8080'"$createSchemaSetBasePath"'' -H 'accept: */*' --form "file=@"/model/subscription.yang"" --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')
          create_anchor_status_code=$(curl --write-out %{http_code} --silent --output /dev/null -X 'POST' 'http://'"$cpsAndNcmpAsHostAddress"':8080'"$createAnchorBasePath"'' -H 'accept: */*' --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')

          echo "create schema set status: $create_schema_set_status_code"
          echo "create anchor status: $create_anchor_status_code"

          if [ "$create_schema_set_status_code" == 201 ] && [ "$create_anchor_status_code" == 201 ]
          then
            echo "Model upload finish!"
            echo "Exiting container ..."
            echo "Bye Bye!"
            break
          fi
        fi
  fi

  if [ $RETRIES == 20 ]
  then
    echo -e "Creating schema set last status:\n $(curl -X 'POST' 'http://'"$cpsAndNcmpAsHostAddress"':8080'"$createSchemaSetBasePath"'' -H 'accept: */*' --form "file=@"/model/subscription.yang"" --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')\n"
    echo -e "Creating anchor last status:\n $(curl -X 'POST' 'http://'"$cpsAndNcmpAsHostAddress"':8080'"$createAnchorBasePath"'' -H 'accept: */*' --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')\n"
    echo -e "\nToo many attempts. Bye Bye!"
    break
  else
    echo "RETRYING ...[ $RETRIES attempt(s) ]"
    RETRIES=$(($RETRIES +1))
    sleep 10
  fi
done