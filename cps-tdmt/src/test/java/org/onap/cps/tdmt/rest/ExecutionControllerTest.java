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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.onap.cps.tdmt.exception.ExecuteException;
import org.onap.cps.tdmt.exception.RecordNotFoundException;
import org.onap.cps.tdmt.model.ExecutionRequest;
import org.onap.cps.tdmt.service.ExecutionBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(ExecutionController.class)
public class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionBusinessLogic executionBusinessLogic;

    private ObjectMapper mapper;
    private String requestJson;
    private String executePath;

    /**
     * Setup variables before test.
     *
     */
    @Before
    public void setup() throws Exception {
        executePath = "/execute/ran-network/getNbr";
        final Map<String, String> input = new HashMap<>();
        input.put("coverageArea", "Zone 1");
        final ExecutionRequest request = new ExecutionRequest(input);
        mapper = new ObjectMapper();
        requestJson = mapper.writeValueAsString(request);
    }

    @Test
    public void testExecuteTemplate() throws Exception {
        final String result = "{\"key\": \"value\"}";
        Mockito.when(executionBusinessLogic
            .executeTemplate(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any()))
            .thenReturn(result);
        mockMvc.perform(post(executePath).contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(requestJson).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(result));

        Mockito.when(executionBusinessLogic
            .executeTemplate(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any()))
            .thenThrow(new RecordNotFoundException("Template does not exist"));
        mockMvc.perform(post(executePath).contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(requestJson).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

        mockMvc.perform(post(executePath).contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content("{\"bad\": \"request\"").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testExecuteTemplateException() throws Exception {
        final String responseJson = "{\n"
            + "  \"message\": \"Error while executing template\",\n"
            + "  \"details\": [\"Response from CPS other than 200: 404\"]\n"
            + "}";

        Mockito.when(executionBusinessLogic
            .executeTemplate(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any()))
            .thenThrow(new ExecuteException("Response from CPS other than 200: 404"));
        mockMvc.perform(post(executePath).contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(requestJson).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(responseJson));
    }
}
