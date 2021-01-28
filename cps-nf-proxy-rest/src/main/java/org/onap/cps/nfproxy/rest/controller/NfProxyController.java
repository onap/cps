/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.nfproxy.rest.controller;

import org.onap.cps.nfproxy.rest.api.NfProxyApi;
import org.onap.cps.spi.exceptions.CpsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.xnf-base-path}")
public class NfProxyController implements NfProxyApi {

    @Override
    public ResponseEntity<Object> helloWorld() {
        return new ResponseEntity<>("Hello World!", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> helloError() {
        throw new CpsException("Example error Message", "Example error description");
    }
}
