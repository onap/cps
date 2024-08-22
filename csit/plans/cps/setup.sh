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
# Modifications Copyright (C) 2021-2024 Nordix Foundation.
#
# Branched from ccsdk/distribution to this repository Feb 23, 2021
#

###################### setup env ############################
# Set env variables for docker compose
export LOCAL_IP=$((ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+') || hostname -I | awk '{print $1}')

source $WORKSPACE/plans/cps/test.properties
export $(cut -d= -f1 $WORKSPACE/plans/cps/test.properties)

###################### setup cps-ncmp ############################
cd $CPS_HOME/docker-compose

# start CPS/NCMP, DMI Plugin, DMI Stub, and PostgreSQL containers with docker compose
# and wait for all containers to be healthy.
docker-compose --profile dmi-service --profile dmi-stub up -d --wait

###################### setup sdnc #######################################
source $WORKSPACE/plans/cps/sdnc/sdnc_setup.sh

###################### setup pnfsim #####################################
docker-compose -f $WORKSPACE/plans/cps/pnfsim/docker-compose.yml up -d

###################### ROBOT Configurations ##########################
# Pass variables required for Robot test suites in ROBOT_VARIABLES
ROBOT_VARIABLES="-v CPS_CORE_HOST:$CPS_CORE_HOST -v CPS_CORE_PORT:$CPS_CORE_PORT -v DMI_HOST:$LOCAL_IP -v DMI_PORT:$DMI_PORT -v DMI_CSIT_STUB_HOST:$LOCAL_IP -v DMI_CSIT_STUB_PORT:$DMI_DEMO_STUB_PORT -v DMI_AUTH_ENABLED:$DMI_AUTH_ENABLED -v DATADIR_CPS_CORE:$WORKSPACE/data/cps-core -v DATADIR_NCMP:$WORKSPACE/data/ncmp -v DATADIR_SUBS_NOTIFICATION:$WORKSPACE/data/subscription-notification --exitonfailure"
