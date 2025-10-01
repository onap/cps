#!/bin/bash
#
# Copyright 2025 OpenInfra Foundation Europe. All rights reserved.
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

echo '================================== k8s info =========================='
kubectl get all -l app=cps-and-ncmp

# Zip and store logs for the containers
chmod +x make-logs-k8s.sh
./make-logs-k8s.sh

echo '================================== uninstalling cps... =========================='
helm uninstall cps