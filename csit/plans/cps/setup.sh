#!/bin/bash
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
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

# Copy docker-compose.yml and application.yml to archives
mkdir -p $WORKSPACE/archives/docker-compose
cp $WORKSPACE/../docker-compose/*.yml $WORKSPACE/archives/docker-compose
cd $WORKSPACE/archives/docker-compose

# Set env variables for docker compose
export LOCAL_IP=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
export DMI_SERVICE_URL=http://$LOCAL_IP:8783
export DB_HOST=$LOCAL_IP
export SDNC_HOST=$LOCAL_IP
export CPS_CORE_HOST=$LOCAL_IP
export DB_USERNAME=cps
export DB_PASSWORD=cps
# Use latest image version
#export VERSION=latest

# download docker-compose of a required version (1.25.0 supports configuration of version 3.7)
curl -L https://github.com/docker/compose/releases/download/1.25.0/docker-compose-`uname -s`-`uname -m` > docker-compose
chmod +x docker-compose

# start CPS and PostgreSQL containers with docker compose
docker network create test_network
./docker-compose up -d

###################### setup sdnc ############################
source $WORKSPACE/plans/cps/sdnc/sdnc_setup.sh

###################### setup pnfsim ##########################
docker-compose -f $WORKSPACE/plans/cps/pnfsim/docker-compose.yml up -d

# Allow time for netconf-pnp-simulator & SDNC to come up fully
sleep 30s

SDNC_TIME_OUT=250
SDNC_INTERVAL=10
SDNC_TIME=0

while [ "$SDNC_TIME" -le "$SDNC_TIME_OUT" ]; do

	# Mount netconf node

	curl --location --request PUT 'http://'"$LOCAL_IP"':8282/restconf/config/network-topology:network-topology/topology/topology-netconf/node/PNFDemo' \
	--header 'Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==' \
	--header 'Content-Type: application/json' \
	--data-raw '{
	  "node": [
		{
		  "node-id": "PNFDemo",
		  "netconf-node-topology:protocol": {
			"name": "TLS"
		  },
		  "netconf-node-topology:host": '"$LOCAL_IP"',
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

	RESPONSE=$( curl --location --request GET 'http://'"$LOCAL_IP"':8282/restconf/config/network-topology:network-topology/topology/topology-netconf' --header 'Authorization: basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==')

	  if [[ "$RESPONSE" == *"PNFDemo"* ]]; then
	    echo "Node mounted in $SDNC_TIME"
		  break;
	  fi

	 sleep $SDNC_INTERVAL
	 SDNC_TIME=$((SDNC_TIME + SDNC_INTERVAL))

done

# Validate CPS service initialization completed via periodic log checking for line like below:
# org.onap.cps.Application ... Started Application in X.XXX seconds

TIME_OUT=300
INTERVAL=5
TIME=0

while [ "$TIME" -le "$TIME_OUT" ]; do
  LOG_FOUND=$( ./docker-compose logs --tail="all" | grep "org.onap.cps.Application" | egrep -c "Started Application in" )

  if [ "$LOG_FOUND" -gt 0 ]; then
    echo "CPS Service started"
    break;
  fi

  echo "Sleep $INTERVAL seconds before next check for CPS initialization (waiting $TIME seconds; timeout is $TIME_OUT seconds)"
  sleep $INTERVAL
  TIME=$((TIME + INTERVAL))
done

if [ "$TIME" -gt "$TIME_OUT" ]; then
   echo "TIME OUT: CPS Service wasn't able to start in $TIME_OUT seconds, setup failed."
   exit 1;
fi

# Pass variables required for Robot test suites in ROBOT_VARIABLES
ROBOT_VARIABLES="-v CPS_HOST:$LOCAL_IP -v CPS_PORT:8883 -v DMI_HOST:$LOCAL_IP -v DMI_PORT:8783 -v MANAGEMENT_PORT:8887 -v DATADIR:$WORKSPACE/data"