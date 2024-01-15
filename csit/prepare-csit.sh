#!/bin/bash -x
#
# Copyright 2019-2021 © Samsung Electronics Co., Ltd.
# Modifications Copyright (C) 2021 Pantheon.tech
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
# This script installs common libraries required by CSIT tests
#
# Branched from ccsdk/distribution to this repository Feb 23, 2021
#

echo "---> prepare-csit.sh"

if [ -z "$WORKSPACE" ]; then
    export WORKSPACE=`git rev-parse --show-toplevel`
fi

TESTPLANDIR=${WORKSPACE}/${TESTPLAN}

# Version should match those used to setup robot-framework in other jobs/stages
# Use pyenv for selecting the python version
if [[ -d "/opt/pyenv" ]]; then
  echo "Setup pyenv:"
  export PYENV_ROOT="/opt/pyenv"
  export PATH="$PYENV_ROOT/bin:$PATH"
  pyenv versions
  if command -v pyenv 1>/dev/null 2>&1; then
    eval "$(pyenv init - --no-rehash)"
    # Choose the latest numeric Python version from installed list
    version=$(pyenv versions --bare | sed '/^[^0-9]/d' | sort -V | tail -n 1)
    pyenv local "${version}"
  fi
fi

# Assume that if ROBOT3_VENV is set and virtualenv with system site packages can be activated,
# ci-management/jjb/integration/include-raw-integration-install-robotframework.sh has already
# been executed

if [ -f ${WORKSPACE}/env.properties ]; then
    source ${WORKSPACE}/env.properties
fi
if [ -f ${ROBOT3_VENV}/bin/activate ]; then
   source ${ROBOT3_VENV}/bin/activate
else
    rm -rf /tmp/ci-management
    rm -f ${WORKSPACE}/env.properties
    cd /tmp
    git clone "https://gerrit.onap.org/r/ci-management"
#    source /tmp/ci-management/jjb/integration/include-raw-integration-install-robotframework-py3.sh
    source ${WORKSPACE}/install-robotframework.sh
fi

# install eteutils
mkdir -p ${ROBOT3_VENV}/src/onap
rm -rf ${ROBOT3_VENV}/src/onap/testsuite

python3 -m pip install --upgrade --extra-index-url="https://nexus3.onap.org/repository/PyPi.staging/simple" 'robotframework-onap==11.0.0.dev17' --pre

echo "Versioning information:"
python3 --version

echo "Installing specific version of the robot framework:"
pip install robotframework==6.1.1

echo "Installing confluent kafka library for robot framework:"
pip install robotframework-confluentkafkalibrary

pip freeze
python3 -m robot.run --version || :