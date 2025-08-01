#  ============LICENSE_START=======================================================
#  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

import subprocess

project = "onap"
release = "master"
version = "master"

author = "Open Network Automation Platform"
# yamllint disable-line rule:line-length
copyright = "ONAP. Licensed under Creative Commons Attribution 4.0 International License"

pygments_style = "sphinx"
html_theme = "sphinx_rtd_theme"
html_theme_options = {
    "style_nav_header_background": "white",
    "sticky_navigation": "False" }
html_logo = "_static/logo_onap_2024.png"
html_favicon = "_static/favicon.ico"
html_static_path = ["_static"]
html_show_sphinx = False

extensions = [
    'sphinx.ext.intersphinx', 'sphinxcontrib.blockdiag', 'sphinx.ext.autosummary',
    'sphinxcontrib.seqdiag', 'sphinxcontrib.swaggerdoc', 'sphinx.ext.graphviz',
    'sphinxcontrib.spelling']

#
# Map to 'latest' if this file is used in 'latest' (master) 'doc' branch.
# Change to {releasename} after you have created the new 'doc' branch.
#

branch = 'latest'

doc_url = 'https://docs.onap.org/projects'
master_doc = 'index'

exclude_patterns = [
    '.tox',
    '/site-packages//LICENSE.rst',
    '/site-packages//licenses/.rst',
    '/site-packages//templates/**/.rst',
    '/site-packages//DESCRIPTION.rst' ]


spelling_word_list_filename='spelling_wordlist.txt'
spelling_lang = "en_GB"

intersphinx_mapping = {}

intersphinx_mapping['onap-cps-ncmp-dmi-plugin'] = ('{}/onap-cps-ncmp-dmi-plugin/en/%s'.format(doc_url) % branch, None)

linkcheck_ignore = [
    'http://localhost',
    'https://example.com',
    'about:config'
]


html_last_updated_fmt = '%d-%b-%y %H:%M'

# Run ScrapeMetrics.py
subprocess.run(["python3", "ScrapeMetrics.py"], check=True)

# Run test_ScrapeMetrics.py
subprocess.run(["python3", "test_ScrapeMetrics.py"], check=True)


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
