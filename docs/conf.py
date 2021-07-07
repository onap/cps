#  ============LICENSE_START=======================================================
#  Copyright (C) 2021 Nordix Foundation
#  ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  ============LICENSE_END=========================================================

from docs_conf.conf import *

branch = 'latest'
master_doc = 'index'
extensions = [
    'sphinx.ext.autosectionlabel',
    'sphinxcontrib.swaggerdoc'
    ]

linkcheck_ignore = [
    'http://localhost',
]

intersphinx_mapping = {}

html_last_updated_fmt = '%d-%b-%y %H:%M'

autosectionlabel_maxdepth = 2

def setup(app):
    app.add_stylesheet("css/ribbon.css")
