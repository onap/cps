#!/bin/bash
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
# Modifications Copyright (C) 2022 Nordix Foundation.
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

    RESPONSE=$(curl --location --request GET 'http://'$1'/actuator/health/readiness')

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
export LOCAL_IP=$((ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+') || hostname -I | awk '{print $1}')

source $WORKSPACE/plans/cps/test.properties
export $(cut -d= -f1 $WORKSPACE/plans/cps/test.properties)

###################### setup cps-ncmp ############################
cd $CPS_HOME/docker-compose

curl -L https://github.com/docker/compose/releases/download/1.29.2/docker-compose-`uname -s`-`uname -m` > docker-compose
chmod +x docker-compose
docker-compose version

# start CPS/NCMP, DMI Plugin, and PostgreSQL containers with docker compose
docker-compose --profile dmi-service up -d

###################### setup sdnc #######################################
source $WORKSPACE/plans/cps/sdnc/sdnc_setup.sh

###################### setup pnfsim #####################################
docker-compose -f $WORKSPACE/plans/cps/pnfsim/docker-compose.yml up -d

###################### verify ncmp-cps health ##########################

check_health $CPS_CORE_HOST:$CPS_CORE_PORT 'cps-ncmp'

###################### verify dmi health ##########################

check_health $DMI_HOST:$DMI_PORT 'dmi-plugin'

###################### ROBOT Configurations ##########################
# Pass variables required for Robot test suites in ROBOT_VARIABLES
ROBOT_VARIABLES="-v CPS_CORE_HOST:$CPS_CORE_HOST -v CPS_CORE_PORT:$CPS_CORE_PORT -v DMI_HOST:$LOCAL_IP -v DMI_PORT:$DMI_PORT -v DMI_CSIT_STUB_HOST:$LOCAL_IP -v DMI_CSIT_STUB_PORT:$DMI_DEMO_STUB_PORT -v DMI_AUTH_ENABLED:$DMI_AUTH_ENABLED -v DATADIR_CPS_CORE:$WORKSPACE/data/cps-core -v DATADIR_NCMP:$WORKSPACE/data/ncmp --exitonfailure"