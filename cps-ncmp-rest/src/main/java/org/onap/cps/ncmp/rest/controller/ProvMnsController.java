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
     * @param className               Class name of the targeted resource
     * @param id                      Identifier of the targeted resource
     * @param resource                Resource representation of the resource to be created or replaced
     * @return {@code ResponseEntity} The representation of the updated resource is returned in the response
     *                                message body.
     */
    @Override
    public ResponseEntity<Resource> classNameidPut(final String className, final String id, final Resource resource) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Reads one or multiple resources.
     *
     * @param className               Class name of the targeted resource
     * @param id                      Identifier of the targeted resource
     * @param scope                   Extends the set of targeted resources beyond the base
     *                                resource identified with the authority and path component of
     *                                the URI.
     * @param filter                  Reduces the targeted set of resources by applying a filter to
     *                                the scoped set of resource representations. Only resources
     *                                representations for which the filter construct evaluates to
     *                                "true" are targeted.
     * @param attributes              Attributes of the scoped resources to be returned. The
     *                                value is a comma-separated list of attribute names.
     * @param fields                  Attribute fields of the scoped resources to be returned. The
     *                                value is a comma-separated list of JSON pointers to the
     *                                attribute fields.
     * @param dataNodeSelector        dataNodeSelector object
     * @return {@code ResponseEntity} The resources identified in the request for retrieval are returned
     *                                in the response message body.
     */
    @Override
    public ResponseEntity<Resource> classNameidGet(final String className, final String id, final Scope scope,
                                                   final String filter, final List<String> attributes,
                                                   final List<String> fields,
                                                   final ClassNameIdGetDataNodeSelectorParameter dataNodeSelector) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Patches (Create, Update or Delete) one or multiple resources.
     *
     * @param className               Class name of the targeted resource
     * @param id                      Identifier of the targeted resource
     * @param resource                Resource representation of the resource to be created or replaced
     * @return {@code ResponseEntity} The updated resource representations are returned in the response message body.
     */
    @Override
    public ResponseEntity<Resource> classNameidPatch(final String className, final String id, final Resource resource) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Delete one or multiple resources.
     *
     * @param className               Class name of the targeted resource
     * @param id                      Identifier of the targeted resource
     * @return {@code ResponseEntity} The response body is empty, HTTP status returned.
     */
    @Override
    public ResponseEntity<Void> classNameidDelete(final String className, final String id) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
