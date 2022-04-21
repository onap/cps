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


from docutils.parsers.rst import directives
from docs_conf.conf import *

#change 'latest' to relevant branch-name once branch has been created
branch = 'jakarta'
doc_url = 'https://docs.onap.org/projects'
master_doc = 'index'

intersphinx_mapping = {}

intersphinx_mapping['onap-cps-ncmp-dmi-plugin'] = ('{}/onap-cps-ncmp-dmi-plugin/en/%s'.format(doc_url) % branch, None)
intersphinx_mapping['onap-cps-cps-temporal'] = ('{}/onap-cps-cps-temporal/en/%s'.format(doc_url) % branch, None)

linkcheck_ignore = [
    'http://localhost',
    'https://example.com',
    'about:config',
    # this URL is not directly reachable and must be configured in the system hosts file.
    'https://portal.api.simpledemo.onap.org:30225/ONAPPORTAL/login.htm',
    # anchor issues
    'https://docs.onap.org/projects/onap-integration/en/latest/docs_usecases_release.html#.*',
    'https://docs.linuxfoundation.org/docs/communitybridge/easycla/contributors/contribute-to-a-gerrit-project#.*',
    'https://docs.onap.org/projects/onap-integration/en/latest/docs_robot.html#docs-robot',
    'https://docs.onap.org/projects/onap-integration/en/latest/docs_usecases_release.html#docs-usecases-release',
    'https://docs.onap.org/projects/onap-integration/en/latest/docs_usecases.html#docs-usecases',
    'https://docs.onap.org/projects/onap-integration/en/latest/usecases/release_non_functional_requirements.html#release-non-functional-requirements',
]


html_last_updated_fmt = '%d-%b-%y %H:%M'


def setup(app):
    app.add_css_file("css/ribbon.css")


needs_extra_options = {
    "target": directives.unchanged,
    "keyword": directives.unchanged,
    "introduced": directives.unchanged,
    "updated": directives.unchanged,
    "impacts": directives.unchanged,
    "validation_mode": directives.unchanged,
    "validated_by": directives.unchanged,
    "test": directives.unchanged,
    "test_case": directives.unchanged,
    "test_file": directives.unchanged,
    "notes": directives.unchanged,
}

needs_id_regex = "^[A-Z0-9]+-[A-Z0-9]+"
needs_id_required = True
needs_title_optional = True

needs_template_collapse = """
.. _{{id}}:

{% if hide == false -%}
.. role:: needs_tag
.. role:: needs_status
.. role:: needs_type
.. role:: needs_id
.. role:: needs_title

.. rst-class:: need
.. rst-class:: need_{{type_name}}

.. container:: need

    `{{id}}` - {{content|indent(4)}}

    .. container:: toggle

        .. container:: header

            Details

{% if status and  status|upper != "NONE" and not hide_status %}        | status: :needs_status:`{{status}}`{% endif %}
{% if tags and not hide_tags %}        | tags: :needs_tag:`{{tags|join("` :needs_tag:`")}}`{% endif %}
{% if keyword %}        | keyword: `{{keyword}}` {% endif %}
{% if target %}        | target: `{{target}}` {% endif %}
{% if introduced %}        | introduced: `{{introduced}}` {% endif %}
{% if updated %}        | updated: `{{updated}}` {% endif %}
{% if impacts %}        | impacts: `{{impacts}}` {% endif %}
{% if validation_mode %}        | validation mode: `{{validation_mode}}` {% endif %}
{% if validated_by %}        | validated by: `{{validated_by}}` {% endif %}
{% if test %}        | test: `{{test}}` {% endif %}
{% if test_case %}        | test case: {{test_case}} {% endif %}
{% if test_file %}        | test file: `{{test_file}}` {% endif %}
{% if notes %}        | notes: `{{notes}}` {% endif %}
        | children: :need_incoming:`{{id}}`
        | parents: :need_outgoing:`{{id}}`
{% endif -%}
"""
