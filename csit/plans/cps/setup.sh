#!/bin/bash
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
# Modifications Copyright (C) 2021-2022 Nordix Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Modifications copyright (c) 2017 AT&T Intellectual Property
# Modifications copyright (c) 2020-2021 Samsung Electronics Co., Ltd.
# Modifications Copyright (C) 2021 Pantheon.tech
# Modifications Copyright (C) 2021 Bell Canada.
# Modifications Copyright (C) 2021 Nordix Foundation.
#
# Branched from ccsdk/distribution to this repository Feb 23, 2021
#

check_health()
{
  TIME_OUT=120
  INTERVAL=5
  TICKER=0

  while [ "$TICKER" -le "$TIME_OUT" ]; do

    RESPONSE=$(curl --location --request GET 'http://'$1'/manage/health/readiness')

    if [[ "$RESPONSE" == *"UP"* ]]; then
      echo "$2 started in $TICKER"
      break;
    fi

    sleep $INTERVAL
    TICKER=$((TICKER + INTERVAL))

  done

  if [ "$TICKER" -ge "$TIME_OUT" ]; then
    echo TIME OUT: $2 session not started in $TIME_OUT seconds... Could cause problems for testing activities...
  fi
}

###################### setup env ############################
# Set env variables for docker compose
export LOCAL_IP=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')

source $WORKSPACE/plans/cps/test.properties
export $(cut -d= -f1 $WORKSPACE/plans/cps/test.properties)

###################### setup cps-ncmp ############################
mkdir -p $WORKSPACE/archives/dc-cps
cp $WORKSPACE/../docker-compose/*.yml $WORKSPACE/archives/dc-cps
cd $WORKSPACE/archives/dc-cps

# download docker-compose of a required version (1.25.0 supports configuration of version 3.7)
curl -L https://github.com/docker/compose/releases/download/1.25.0/docker-compose-`uname -s`-`uname -m` > docker-compose
chmod +x docker-compose

# start CPS and PostgreSQL containers with docker compose
./docker-compose up -d

###################### setup onap-dmi-plugin ############################

cd $WORKSPACE/archives
git clone "https://gerrit.onap.org/r/cps/ncmp-dmi-plugin"
mkdir -p $WORKSPACE/archives/dc-dmi
cat $WORKSPACE/archives/ncmp-dmi-plugin/docker-compose/docker-compose.yml
cp $WORKSPACE/archives/ncmp-dmi-plugin/docker-compose/*.yml $WORKSPACE/archives/dc-dmi
cd $WORKSPACE/archives/dc-dmi
# copy docker-compose (downloaded already for cps)
cp $WORKSPACE/archives/dc-cps/docker-compose .
chmod +x docker-compose
./docker-compose up -d

###################### setup sdnc #######################################
source $WORKSPACE/plans/cps/sdnc/sdnc_setup.sh

###################### setup pnfsim #####################################
docker-compose -f $WORKSPACE/plans/cps/pnfsim/docker-compose.yml up -d

# Allow time for netconf-pnp-simulator & SDNC to come up fully
sleep 30s

###################### mount pnf-sim as PNFDemo ##########################
SDNC_TIME_OUT=250
SDNC_INTERVAL=10
SDNC_TIME=0

while [ "$SDNC_TIME" -le "$SDNC_TIME_OUT" ]; do

   # Mount netconf node
   curl --location --request PUT 'http://'$SDNC_HOST:$SDNC_PORT'/restconf/config/network-topology:network-topology/topology/topology-netconf/node/PNFDemo' \
   --header 'Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==' \
   --header 'Content-Type: application/json' \
   --data-raw '{
     "node": [
     {
       "node-id": "PNFDemo",
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

   curl --location --request PUT 'http://'$SDNC_HOST:$SDNC_PORT'/restconf/config/network-topology:network-topology/topology/topology-netconf/node/PNFDemo2' \
   --header 'Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==' \
   --header 'Content-Type: application/json' \
   --data-raw '{
     "node": [
     {
       "node-id": "PNFDemo2",
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

   curl --location --request PUT 'http://'$SDNC_HOST:$SDNC_PORT'/restconf/config/network-topology:network-topology/topology/topology-netconf/node/PNFDemo3' \
   --header 'Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==' \
   --header 'Content-Type: application/json' \
   --data-raw '{
     "node": [
     {
       "node-id": "PNFDemo3",
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

  if [[ "$RESPONSE" == *"PNFDemo"* ]]; then
    echo "Node mounted in $SDNC_TIME"
    break;
  fi

  sleep $SDNC_INTERVAL
  SDNC_TIME=$((SDNC_TIME + SDNC_INTERVAL))

done

###################### verify ncmp-cps health ##########################

check_health $CPS_CORE_HOST:$CPS_CORE_MANAGEMENT_PORT 'cps-ncmp'

###################### verify dmi health ##########################

check_health $DMI_HOST:$DMI_MANAGEMENT_PORT 'dmi-plugin'

###################### ROBOT Configurations ##########################
# Pass variables required for Robot test suites in ROBOT_VARIABLES
ROBOT_VARIABLES="-v CPS_CORE_HOST:$CPS_CORE_HOST -v CPS_CORE_PORT:$CPS_CORE_PORT -v DMI_HOST:$LOCAL_IP -v DMI_PORT:$DMI_PORT -v CPS_CORE_MANAGEMENT_PORT:$CPS_CORE_MANAGEMENT_PORT  -v SDNC_HOST:$SDNC_HOST -v SDNC_PORT:$SDNC_PORT -v DATADIR:$WORKSPACE/data --exitonfailure"