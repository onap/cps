/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
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

package org.onap.cps.ri.utils;

import lombok.RequiredArgsConstructor;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.SchemaSetEntity;
import org.onap.cps.ri.models.YangResourceEntity;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class CpsSessionFactory {

    private final Configuration hibernateConfiguration;

    private SessionFactory sessionFactory = null;

    /**
     * Open a session from session factory.
     *
     * @return session
     * @throws HibernateException hibernate exception
     */
    public Session openSession() throws HibernateException {
        return getSessionFactory().openSession();
    }

    /**
     * Close session factory.
     *
     * @throws HibernateException hibernate exception
     */
    public void closeSessionFactory() throws HibernateException {
        getSessionFactory().close();
    }

    private SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = hibernateConfiguration
                    .addAnnotatedClass(AnchorEntity.class)
                    .addAnnotatedClass(DataspaceEntity.class)
                    .addAnnotatedClass(SchemaSetEntity.class)
                    .addAnnotatedClass(YangResourceEntity.class)
                    .buildSessionFactory();
        }
        return sessionFactory;
    }
}
