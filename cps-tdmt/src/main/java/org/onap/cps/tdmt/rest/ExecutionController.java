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

import javax.validation.Valid;
import org.onap.cps.tdmt.model.ExecutionRequest;
import org.onap.cps.tdmt.service.ExecutionBusinessLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionController {

    @Autowired
    private ExecutionBusinessLogic executionBusinessLogic;


    @PostMapping(path = "/execute/{schemaSet}/{id}")
    public ResponseEntity<String> executeTemplate(@Valid @PathVariable final String schemaSet,
        @Valid @PathVariable final String id,
        @Valid @RequestBody final ExecutionRequest request) {
        final String result = executionBusinessLogic.executeTemplate(schemaSet, id, request);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
