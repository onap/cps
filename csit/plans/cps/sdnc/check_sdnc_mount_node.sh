# ============LICENSE_START=======================================================
# Copyright (C) 2023 Nordix Foundation
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================

# WAIT 10 minutes maximum and test every 30 seconds if SDNC is up using HealthCheck API
TIME_OUT=600
INTERVAL=30
TIME=0
while [ "$TIME" -lt "$TIME_OUT" ]; do
  response=$(curl --write-out '%{http_code}' --silent --output /dev/null -H "Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==" -X POST -H "X-FromAppId: csit-sdnc" -H "X-TransactionId: csit-sdnc" -H "Accept: application/json" -H "Content-Type: application/json" http://$SDNC_HOST:$SDNC_PORT/restconf/operations/SLI-API:healthcheck );
  echo $response

  if [ "$response" == "200" ]; then
    echo SDNC started in $TIME seconds
    break;
  fi

  echo Sleep: $INTERVAL seconds before testing if SDNC is up. Total wait time up now is: $TIME seconds. Timeout is: $TIME_OUT seconds
  sleep $INTERVAL
  TIME=$(($TIME+$INTERVAL))
done

if [ "$TIME" -ge "$TIME_OUT" ]; then
   echo TIME OUT: karaf session not started in $TIME_OUT seconds... Could cause problems for testing activities...
fi

###################### mount pnf-sim as PNFDemo ##########################
SDNC_TIME_OUT=250
SDNC_INTERVAL=10
SDNC_TIME=0

while [ "$SDNC_TIME" -le "$SDNC_TIME_OUT" ]; do

  # Mount netconf node
  curl --location --request PUT 'http://'$SDNC_HOST:$SDNC_PORT'/restconf/config/network-topology:network-topology/topology/topology-netconf/node/ietfYang-PNFDemo' \
  --header 'Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "node": [
    {
      "node-id": "ietfYang-PNFDemo",
      "netconf-node-topology:protocol": {
      "name": "TLS"
      },
      "netconf-node-topology:host": "'$LOCAL_IP'",
      "netconf-node-topology:key-based": {
      "username": "netconf",
      "key-id": "ODL_private_key_0"
      },
      "netconf-node-topology:port": 6512,
      "netconf-node-topology:tcp-only": false,
      "netconf-node-topology:max-connection-attempts": 5
    }
    ]
  }'

   # Verify node has been mounted

  RESPONSE=$( curl --location --request GET 'http://'$SDNC_HOST:$SDNC_PORT'/restconf/config/network-topology:network-topology/topology/topology-netconf' --header 'Authorization: basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==')

  if [[ "$RESPONSE" == *"ietfYang-PNFDemo"* ]]; then
    echo "Node mounted in $SDNC_TIME"
    sleep 10
    break;
  fi

  sleep $SDNC_INTERVAL
  SDNC_TIME=$((SDNC_TIME + SDNC_INTERVAL))

done