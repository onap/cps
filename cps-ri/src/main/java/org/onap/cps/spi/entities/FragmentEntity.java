/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2023 Nordix Foundation.
 * Modifications Copyright (C) 2021 Pantheon.tech
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.entities;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * Entity to store a fragment.
 */
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "fragment")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FragmentEntity implements Serializable {

    private static final long serialVersionUID = 7737669789097119667L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fragment_id_seq_generator")
    @SequenceGenerator(name = "fragment_id_seq_generator", sequenceName = "fragment_id_seq", allocationSize = 100)
    private Long id;

    @NotNull
    @Column(columnDefinition = "text")
    @EqualsAndHashCode.Include
    private String xpath;

    @Column(name = "parent_id")
    private Long parentId;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private String attributes;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anchor_id")
    @EqualsAndHashCode.Include
    private AnchorEntity anchor;

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Set<FragmentEntity> childFragments;
}
