#!/bin/bash

echo "Getting ready to upload model for subscription events ..."

RETRIES=0
while :
do
  sleep 30
  cpsAndNcmpAsHostAddress=$(ping -c1 cps-and-ncmp | sed -nE 's/^PING[^(]+\(([^)]+)\).*/\1/p')

  echo "Checking that NCMP dataspace exists ..."
  ncmpDataspaceExists=$(curl --write-out %{http_code} --silent --output /dev/null -X 'GET' 'http://'"$cpsAndNcmpAsHostAddress"':8080/cps/api/v2/admin/dataspaces/NCMP-Admin' -H 'accept: */*' --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')

  echo "NCMP dataspace exist:  $ncmpDataspaceExists"

  if [ $ncmpDataspaceExists == 200 ]
  then
    echo "Uploading model ..."
    create_schema_set_status_code=$(curl --write-out %{http_code} --silent --output /dev/null -X 'POST' 'http://'"$cpsAndNcmpAsHostAddress"':8080/cps/api/v2/dataspaces/NCMP-Admin/schema-sets?schema-set-name=subscriptions' -H 'accept: */*' --form "file=@"/model/subscription.yang"" --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')
    create_anchor_status_code=$(curl --write-out %{http_code} --silent --output /dev/null -X 'POST' 'http://'"$cpsAndNcmpAsHostAddress"':8080/cps/api/v2/dataspaces/NCMP-Admin/anchors?schema-set-name=subscriptions&anchor-name=AVC-subscriptions' -H 'accept: */*' --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=')

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

  if [ $RETRIES == 10 ]
  then
    break
    echo "Too many attempts. Bye Bye!"
  else
    echo "RETRYING ...[ $RETRIES attempt(s) ]"
    RETRIES=$(($RETRIES +1))
    sleep 10
  fi
done