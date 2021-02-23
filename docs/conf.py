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
