#!/bin/bash
#
# ============LICENSE_START=======================================================
#   Copyright (C) 2020 Nordix Foundation.
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

# @author Rahul Tyagi (rahul.tyagi@est.tech)
# setup sdnc

export SDNC_CERT_PATH=$WORKSPACE/plans/sdn/certs

#docker pull $NEXUS_DOCKER_REPO/onap/sdnc-ansible-server-image:$SDNC_IMAGE_TAG
#docker tag $NEXUS_DOCKER_REPO/onap/sdnc-ansible-server-image:$SDNC_IMAGE_TAG onap/sdnc-ansible-server-image:latest

#start SDNC containers with docker compose and configuration from docker-compose.yml
docker-compose -f $WORKSPACE/plans/sdn/docker-compose.yml up -d

# WAIT 10 minutes maximum and test every 5 seconds if SDNC is up using HealthCheck API
TIME_OUT=1000
INTERVAL=30
TIME=0
while [ "$TIME" -lt "$TIME_OUT" ]; do
  response=$(curl --write-out '%{http_code}' --silent --output /dev/null -H "Authorization: Basic YWRtaW46S3A4Yko0U1hzek0wV1hsaGFrM2VIbGNzZTJnQXc4NHZhb0dHbUp2VXkyVQ==" -X POST -H "X-FromAppId: csit-sdnc" -H "X-TransactionId: csit-sdnc" -H "Accept: application/json" -H "Content-Type: application/json" http://localhost:8282/restconf/operations/SLI-API:healthcheck );
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