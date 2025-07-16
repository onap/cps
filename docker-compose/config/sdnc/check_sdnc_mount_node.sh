#!/bin/sh
# ============LICENSE_START=======================================================
# Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

set -x  # Enable command echoing
apk --no-cache add curl

SDNC_HOST=${SDNC_HOST:-'sdnc'}
SDNC_PORT=${SDNC_PORT:-8181}
SDNC_AUTH_HEADER=${SDNC_AUTH_HEADER:-'Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ=='}
PNF_SIM_HOST=${PNF_SIM_HOST:-'pnf-simulator'}
PNF_SIM_PORT=${PNF_SIM_PORT:-6513}
NODE_ID=${NODE_ID:-'ietfYang-PNFDemo'}

echo "Attempting to mount node with id '$NODE_ID' to SDNC using RestConf"
curl --request PUT "http://$SDNC_HOST:$SDNC_PORT/rests/data/network-topology:network-topology/topology=topology-netconf/node=$NODE_ID" \
--silent --location \
--header "$SDNC_AUTH_HEADER" \
--header 'Content-Type: application/json' \
--data-raw '{
  "node": [
  {
    "node-id": "'$NODE_ID'",
    "netconf-node-topology:protocol": {
    "name": "TLS"
    },
    "netconf-node-topology:host": "'$PNF_SIM_HOST'",
    "netconf-node-topology:key-based": {
    "username": "netconf",
    "key-id": "ODL_private_key_0"
    },
    "netconf-node-topology:port": '$PNF_SIM_PORT',
    "netconf-node-topology:tcp-only": false,
    "netconf-node-topology:max-connection-attempts": 5
  }
  ]
}'

# Verify node has been mounted
RESPONSE=$(curl --silent --location --request GET "http://$SDNC_HOST:$SDNC_PORT/rests/data/network-topology:network-topology/topology=topology-netconf?content=config" --header "$SDNC_AUTH_HEADER")

if echo "$RESPONSE" | grep -q "$NODE_ID"; then
  echo "Node mounted successfully"
  exit 0
else
  echo "Could not mount node to SNDC"
  exit 1
fi
