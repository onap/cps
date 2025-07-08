/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.controller;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.rest.provmns.api.DefaultApi;
import org.onap.cps.ncmp.rest.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.rest.provmns.model.Resource;
import org.onap.cps.ncmp.rest.provmns.model.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.provmns-base-path}")
@RequiredArgsConstructor
public class ProvMnsController implements DefaultApi {

    /**
     * Replaces a complete single resource or creates it if it does not exist.
     *
     * @param className               name of the class
     * @param id                      id
     * @param resource                resource object
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Resource> classNameidPut(final String className, final String id, final Resource resource) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Reads one or multiple resources.
     *
     * @param className               name of the class
     * @param id                      id
     * @param scope                   scope object
     * @param filter                  filter string
     * @param attributes              attributes list
     * @param fields                  fields list
     * @param dataNodeSelector        dataNodeSelector object
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Resource> classNameidGet(final String className, final String id, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Patches one or multiple resources.
     *
     * @param className               name of the class
     * @param id                      id
     * @param resource                resource object
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Resource> classNameidPatch(final String className, final String id, final Resource resource) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Delete one or multiple resources.
     *
     * @param className               name of the class
     * @param id                      id
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Void> classNameidDelete(final String className, final String id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
