#!/bin/bash -x
#
# Copyright 2016-2017 Huawei Technologies Co., Ltd.
# Modification Copyright 2019-2021 © Samsung Electronics Co., Ltd.
# Modification Copyright (C) 2024 Nordix Foundation.
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
# $1 project/functionality
# $2 robot options

# Branched from ccsdk/distribution to this repository Feb 23, 2021

echo "---> run-csit.sh"

WORKDIR=$(mktemp -d --suffix=-robot-workdir)

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

#
# functions
#

# relax set options so the sourced file will not fail
# the responsibility is shifted to the sourced file...
function relax_set() {
    set +e
    set +o pipefail
}

# load the saved set options
function load_set() {
    _setopts="$-"

    # bash shellopts
    for i in $(echo "$SHELLOPTS" | tr ':' ' ') ; do
        set +o ${i}
    done
    for i in $(echo "$RUN_CSIT_SHELLOPTS" | tr ':' ' ') ; do
        set -o ${i}
    done

    # other options
    for i in $(echo "$_setopts" | sed 's/./& /g') ; do
        set +${i}
    done
    set -${RUN_CSIT_SAVE_SET}
}

# wrapper for sourcing a file
function source_safely() {
    [ -z "$1" ] && return 1
    relax_set
    . "$1"
    load_set
}
# Activate the virtualenv containing all the required libraries installed by prepare-csit.sh
source_safely "${ROBOT3_VENV}/bin/activate"

function on_exit(){
    rc=$?
    if [[ ${WORKSPACE} ]]; then
        if [[ ${WORKDIR} ]]; then
            rsync -av "$WORKDIR/" "$WORKSPACE/archives/$TESTPLAN"
        fi
        # Record list of active docker containers
        docker ps --format "{{.Image}}" > "$WORKSPACE/archives/$TESTPLAN/_docker-images.log"

        # show memory consumption after all docker instances initialized
        docker_stats | tee "$WORKSPACE/archives/$TESTPLAN/_sysinfo-2-after-robot.txt"
    fi
    # Run teardown script plan if it exists
    cd "${TESTPLANDIR}"
    TEARDOWN="${TESTPLANDIR}/teardown.sh"
    if [ -f "${TEARDOWN}" ]; then
        echo "Running teardown script ${TEARDOWN}"
        source_safely "${TEARDOWN}"
    fi
    # TODO: do something with the output
     exit $rc
}
# ensure that teardown and other finalizing steps are always executed
trap on_exit EXIT

function docker_stats(){
    #General memory details
    echo "> top -bn1 | head -3"
    top -bn1 | head -3
    echo

    echo "> free -h"
    free -h
    echo

    #Memory details per Docker
    echo "> docker ps"
    docker ps
    echo

    echo "> docker stats --no-stream"
    docker stats --no-stream
    echo
}

# save current set options
function save_set() {
    RUN_CSIT_SAVE_SET="$-"
    RUN_CSIT_SHELLOPTS="$SHELLOPTS"
}

# set options for quick bailout when error
function harden_set() {
    set -xeo pipefail
    set +u # enabled it would probably fail too many often
}

function run_test_plan() {
    testplan=$1

    cd "$WORKDIR"
    echo "Reading the testplan:"
    cat "${TESTPLANDIR}/${testplan}.txt" | egrep -v '(^[[:space:]]*#|^[[:space:]]*$)' | sed "s|^|${WORKSPACE}/tests/|" > ${testplan}.txt
    cat ${testplan}.txt
    SUITES=$( xargs -a ${testplan}.txt )

    python3 -m robot.run -N ${TESTPLAN} -v WORKSPACE:/tmp --legacy-output ${ROBOT_VARIABLES} ${TESTOPTIONS} ${SUITES}
    RESULT=$?
    load_set
    echo "RESULT: $RESULT"
    return $RESULT
}

#
# main
#

# set and save options for quick failure
harden_set && save_set

if [ $# -eq 0 ]
then
    echo
    echo "Usage: $0 plans/<project>/<functionality> [<robot-options>]"
    echo
    echo "    <project>, <functionality>, <robot-options>:  "
    echo "        The same values as for the '{project}-csit-{functionality}' JJB job template."
    echo
    exit 1
fi

if [ -z "$WORKSPACE" ]; then
    export WORKSPACE=$(git rev-parse --show-toplevel)
fi

if [ -f "${WORKSPACE}/${1}/testplanCps.txt" ]; then
    export TESTPLAN="${1}"
else
    echo "testplan not found: ${WORKSPACE}/${TESTPLAN}/testplanCps.txt or testplanNcmp.txt"
    exit 2
fi

export TESTOPTIONS="${2}"

rm -rf "$WORKSPACE/archives/$TESTPLAN"
mkdir -p "$WORKSPACE/archives/$TESTPLAN"

TESTPLANDIR="${WORKSPACE}/${TESTPLAN}"

# Run installation of prerequired libraries
source_safely "${WORKSPACE}/prepare-csit.sh"

# Use robot framework working directory
cd "${WORKDIR}"

# Add csit scripts to PATH
export PATH="${PATH}:${WORKSPACE}/docker/scripts:${WORKSPACE}/scripts:${ROBOT3_VENV}/bin"
export SCRIPTS="${WORKSPACE}/scripts"
export ROBOT_VARIABLES=

# Sign in to nexus3 docker repo
docker login -u docker -p docker nexus3.onap.org:10001

# Run setup script plan if it exists
cd "${TESTPLANDIR}"
SETUP="${TESTPLANDIR}/setup.sh"
if [ -f "${SETUP}" ]; then
    echo "Running setup script ${SETUP}"
    source_safely "${SETUP}"
fi

# show memory consumption after all docker instances initialized
docker_stats | tee "$WORKSPACE/archives/$TESTPLAN/_sysinfo-1-after-setup.txt"

# Run test plan
echo ROBOT_VARIABLES="${ROBOT_VARIABLES}"
echo "Starting Robot test suites ${SUITES} ..."
relax_set

echo "Versioning information:"
python3 --version
pip freeze
python3 -m robot.run --version || :

run_test_plan "testplanCps"
CPSRESULT="$?"

cd "${TESTPLANDIR}"
checkandmount="${TESTPLANDIR}/sdnc/check_sdnc_mount_node.sh"
if [ -f "${checkandmount}" ]; then
    echo "Running check_sdnc_mount_node script ${checkandmount}"
    source_safely "${checkandmount}"
fi

run_test_plan "testplanNcmp"
NCMPRESULT="$?"

# Note that the final steps are done in on_exit function after this exit!
exit $CPSRESULT || $NCMPRESULT
