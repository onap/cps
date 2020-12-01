/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.spi.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity to store a yang module.
 */
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "module")
public class Module implements Serializable {

    private static final long serialVersionUID = -748666970938314895L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column
    private String moduleContent;

    @NotNull
    @Column
    private String revision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataspace_id", referencedColumnName = "ID")
    private Dataspace dataspace;

    @NotNull
    @Column
    private String namespace;

    /**
     * Initialize a module entity.
     *
     * @param namespace     the module namespace.
     * @param moduleContent the module content.
     * @param revision      the revision number of the module.
     * @param dataspace     the dataspace related to the module.
     */
    public Module(final String namespace, final String moduleContent, final String revision,
            final Dataspace dataspace) {
        this.namespace = namespace;
        this.moduleContent = moduleContent;
        this.revision = revision;
        this.dataspace = dataspace;
    }
}