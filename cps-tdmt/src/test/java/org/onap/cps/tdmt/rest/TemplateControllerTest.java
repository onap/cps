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

package org.onap.cps.tdmt.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.onap.cps.tdmt.exception.RecordNotFoundException;
import org.onap.cps.tdmt.model.Template;
import org.onap.cps.tdmt.model.TemplateRequest;
import org.onap.cps.tdmt.service.TemplateBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(TemplateController.class)
public class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateBusinessLogic templateBusinessLogic;

    private ObjectMapper mapper;

    private Template template;

    @Before
    public void setup() {
        mapper = new ObjectMapper();
        template = new Template("getNbr", "ran-network", "sample");
    }

    @Test
    public void testCreateTemplate() throws Exception {
        final TemplateRequest emptyTemplateRequest = new TemplateRequest();
        emptyTemplateRequest.setId("getNbr");
        emptyTemplateRequest.setSchemaSet("ran-network");
        final String emptyTemplateJson = mapper.writeValueAsString(emptyTemplateRequest);
        mockMvc.perform(
            post("/templates").contentType(MediaType.APPLICATION_JSON).characterEncoding("utf-8")
                .content(emptyTemplateJson).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        final TemplateRequest request = new TemplateRequest("getNbr", "ran-network", "sample");
        final String templateJson = mapper.writeValueAsString(request);
        Mockito.when(templateBusinessLogic.createTemplate(ArgumentMatchers.any()))
            .thenReturn(template);
        mockMvc.perform(
            post("/templates").contentType(MediaType.APPLICATION_JSON).characterEncoding("utf-8")
                .content(templateJson).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().json(templateJson));
    }

    @Test
    public void testGetAllTemplates() throws Exception {
        final List<Template> templates = new ArrayList<>();
        templates.add(template);
        final String templatesJson = mapper.writeValueAsString(templates);
        Mockito.when(templateBusinessLogic.getAllTemplates()).thenReturn(templates);
        mockMvc.perform(get("/templates").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(templatesJson));

        Mockito.when(templateBusinessLogic.getAllTemplates()).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/templates").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

    }

    @Test
    public void testGetTemplate() throws Exception {
        final String templateJson = mapper.writeValueAsString(template);
        Mockito.when(templateBusinessLogic.getTemplate(ArgumentMatchers.any()))
            .thenReturn(template);
        mockMvc.perform(get("/templates/ran-network/getNbr").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(templateJson));

        Mockito.when(templateBusinessLogic.getTemplate(ArgumentMatchers.any()))
            .thenThrow(new RecordNotFoundException("Template not found"));
        mockMvc.perform(get("/templates/ran-network/getNbr").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteTemplate() throws Exception {
        Mockito.doNothing().when(templateBusinessLogic).deleteTemplate(ArgumentMatchers.any());
        mockMvc.perform(delete("/templates/ran-network/getNbr").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        Mockito.doThrow(new RecordNotFoundException("Template not found"))
            .when(templateBusinessLogic)
            .deleteTemplate(ArgumentMatchers.any());
        mockMvc.perform(delete("/templates/ran-network/getNbr").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

}
