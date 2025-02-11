#!/bin/bash
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
# Modifications copyright (c) 2017 AT&T Intellectual Property
# Modifications copyright (c) 2020-2021 Samsung Electronics Co., Ltd.
# Modifications Copyright (C) 2021 Pantheon.tech
# Modifications Copyright (C) 2021 Bell Canada.
# Modifications Copyright (C) 2021-2025 Nordix Foundation.
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

###################### setup cps-ncmp ############################
cd $CPS_HOME/docker-compose

# start CPS/NCMP, DMI Plugin, and PostgreSQL containers with docker compose, waiting for all containers to be healthy
docker-compose --profile dmi-service up -d --quiet-pull --wait || exit 1

###################### ROBOT Configurations ##########################
# Pass variables required for Robot test suites in ROBOT_VARIABLES
ROBOT_VARIABLES="\
-v CPS_CORE_HOST:localhost \
-v CPS_CORE_PORT:8883 \
-v DMI_HOST:ncmp-dmi-plugin \
-v DMI_PORT:8080 \
-v DMI_CSIT_STUB_HOST:ncmp-dmi-plugin-demo-and-csit-stub \
-v DMI_CSIT_STUB_PORT:8092 \
-v DATADIR_CPS_CORE:$WORKSPACE/data/cps-core \
-v DATADIR_NCMP:$WORKSPACE/data/ncmp \
-v DATADIR_SUBS_NOTIFICATION:$WORKSPACE/data/subscription-notification"
