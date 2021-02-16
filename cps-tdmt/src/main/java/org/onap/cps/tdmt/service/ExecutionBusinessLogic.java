/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 Wipro Limited.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.tdmt.service;

import com.hubspot.jinjava.Jinjava;
import java.util.Map;
import java.util.Optional;
import org.onap.cps.tdmt.client.CpsRestClient;
import org.onap.cps.tdmt.db.TemplateRepository;
import org.onap.cps.tdmt.exception.CpsException;
import org.onap.cps.tdmt.exception.ExecuteException;
import org.onap.cps.tdmt.exception.RecordNotFoundException;
import org.onap.cps.tdmt.model.AppConfiguration;
import org.onap.cps.tdmt.model.ExecutionRequest;
import org.onap.cps.tdmt.model.Template;
import org.onap.cps.tdmt.model.TemplateId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExecutionBusinessLogic {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private AppConfiguration appConfiguration;

    @Autowired
    private CpsRestClient cpsRestClient;

    /**
     * Execute a template stored in the database.
     *
     * @param schemaSet schema set
     * @param id id
     * @return result response from the execution of template
     */
    public String executeTemplate(final String schemaSet, final String id, final ExecutionRequest request) {

        final Optional<Template> template = templateRepository.findById(new TemplateId(id, schemaSet));
        if (template.isPresent()) {
            return execute(template.get(), request.getInput());
        } else {
            throw new RecordNotFoundException("Template does not exist");
        }
    }

    private String execute(final Template template, final Map<String, String> input) {
        final String anchor = appConfiguration.getSchemaToAnchor().get(template.getSchemaSet());
        if (anchor == null) {
            throw new ExecuteException("Anchor not found for the schema");
        }
        final Jinjava jinja = new Jinjava();
        final String xpath = jinja.render(template.getXpathTemplate(), input);
        try {
            return cpsRestClient.fetchNode(anchor, xpath);
        } catch (final CpsException e) {
            throw new ExecuteException(e.getLocalizedMessage());
        }
    }

}
