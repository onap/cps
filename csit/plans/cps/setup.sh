#!/bin/bash
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
# Modifications copyright (c) 2017 AT&T Intellectual Property
# Modifications copyright (c) 2020-2021 Samsung Electronics Co., Ltd.
# Modifications Copyright (C) 2021 Pantheon.tech
# Modifications Copyright (C) 2021 Bell Canada.
# Modifications Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
# Branched from ccsdk/distribution to this repository Feb 23, 2021
#

###################### setup env ############################
# Set env variables for docker compose
export LOCAL_IP=localhost

source $WORKSPACE/plans/cps/test.properties
export $(cut -d= -f1 $WORKSPACE/plans/cps/test.properties)

###################### setup cps-and-ncmp with dmi-services ############################
cd $CPS_HOME/docker-compose

# Start default docker-compose and dmi-services, and wait for all containers to be healthy.
# Note: Since -f flag is used for dmi-services.yml, default docker-compose file should also be binded with -f flag,
# otherwise cps base services won't be running!
docker-compose -f docker-compose.yml -f dmi-services.yml up -d --quiet-pull --wait || exit 1

###################### ROBOT Configurations ##########################
# Pass variables required for Robot test suites in ROBOT_VARIABLES.
ROBOT_VARIABLES="\
-v CPS_CORE_HOST:$CPS_CORE_HOST \
-v CPS_CORE_PORT:$CPS_CORE_PORT \
-v DMI_HOST:$DMI_HOST \
-v DMI_PORT:$DMI_PORT \
-v DMI_CSIT_STUB_HOST:$DMI_DEMO_STUB_HOST \
-v DMI_CSIT_STUB_PORT:$DMI_DEMO_STUB_PORT \
-v DATADIR_CPS_CORE:$WORKSPACE/data/cps-core \
-v DATADIR_NCMP:$WORKSPACE/data/ncmp \
-v DATADIR_SUBS_NOTIFICATION:$WORKSPACE/data/subscription-notification"
