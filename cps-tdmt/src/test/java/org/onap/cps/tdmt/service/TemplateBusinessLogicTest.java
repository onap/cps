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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.onap.cps.tdmt.db.TemplateRepository;
import org.onap.cps.tdmt.exception.RecordNotFoundException;
import org.onap.cps.tdmt.model.Template;
import org.onap.cps.tdmt.model.TemplateId;
import org.onap.cps.tdmt.model.TemplateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class TemplateBusinessLogicTest {

    @TestConfiguration
    static class TemplateBusinessLogicTestContextConfiguration {

        @Bean
        public TemplateBusinessLogic templateBusinessLogic() {
            return new TemplateBusinessLogic();
        }
    }

    @Autowired
    private TemplateBusinessLogic templateBusinessLogic;

    @MockBean
    private TemplateRepository templateRepository;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Template template;
    private TemplateId templateId;

    @Before
    public void setup() {
        template = new Template("getNbr", "ran-network", "sample");
        final TemplateId templateId = new TemplateId("getNbr", "ran-network");
    }

    @Test
    public void testCreateTemplate() throws Exception {
        final TemplateRequest templateRequest = new TemplateRequest("getNbr", "ran-network", "sample");
        Mockito.when(templateRepository.save(ArgumentMatchers.any())).thenReturn(template);
        assertEquals(template, templateBusinessLogic.createTemplate(templateRequest));
    }

    @Test
    public void testGetAllTemplates() throws Exception {
        final List<Template> templates = new ArrayList<>();
        templates.add(template);
        Mockito.when(templateRepository.findAll()).thenReturn(templates);
        assertEquals(templates, templateBusinessLogic.getAllTemplates());
    }

    @Test
    public void testGetTemplate() throws Exception {
        Mockito.when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        assertEquals(template, templateBusinessLogic.getTemplate(templateId));

        Mockito.when(templateRepository.findById(ArgumentMatchers.any()))
            .thenReturn(Optional.empty());
        exception.expect(RecordNotFoundException.class);
        exception.expectMessage("Template not found for given id and schema");
        templateBusinessLogic.getTemplate(new TemplateId("getNbr", "empty-schema"));
    }

    @Test
    public void testDeleteTemplate() throws Exception {
        Mockito.when(templateRepository.existsById(templateId)).thenReturn(true);
        templateBusinessLogic.deleteTemplate(templateId);
        verify(templateRepository, times(1)).deleteById(templateId);

        Mockito.when(templateRepository.existsById(ArgumentMatchers.any())).thenReturn(false);
        exception.expect(RecordNotFoundException.class);
        exception.expectMessage("Delete failed. Could not find template with specified id");
        templateBusinessLogic.deleteTemplate(new TemplateId("getNbr", "empty-schema"));
    }
}
