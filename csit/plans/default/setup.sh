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
#
# Branched from ccsdk/distribution to this repository Feb 23, 2021
#

# Copy docker-compose.yml and application.yml to archives
mkdir -p $WORKSPACE/archives/docker-compose
cp $WORKSPACE/../docker-compose/*.yml $WORKSPACE/archives/docker-compose
cd $WORKSPACE/archives/docker-compose

# Set env variables for docker compose
export DB_HOST=dbpostgresql
export DB_USERNAME=cps
export DB_PASSWORD=cps
# Use latest image version
export VERSION=latest

# download docker-compose of a required version (1.25.0 supports configuration of version 3.7)
curl -L https://github.com/docker/compose/releases/download/1.25.0/docker-compose-`uname -s`-`uname -m` > docker-compose
chmod +x docker-compose

# start CPS and PostgreSQL containers with docker compose
./docker-compose up -d

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

# TODO localhost works on a local environment, check if it's ok on jenkins
CPS_HOST="http://localhost:8883"

# Pass variables required for Robot test suites in ROBOT_VARIABLES
ROBOT_VARIABLES="-v CPS_HOST:$CPS_HOST -v DATADIR:$WORKSPACE/data"

