/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.spi.config;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class CpsSessionFactory {

    private SessionFactory sessionFactory = null;

    /**
     * Open a session from session factory.
     *
     * @return session
     * @throws HibernateException hibernate exception
     */
    public Session openSession() throws HibernateException {
        if (sessionFactory == null) {
            sessionFactory = new org.hibernate.cfg.Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(AnchorEntity.class)
                .addAnnotatedClass(DataspaceEntity.class)
                .addAnnotatedClass(SchemaSetEntity.class)
                .addAnnotatedClass(YangResourceEntity.class)
                .buildSessionFactory();
        }
        return sessionFactory.openSession();
    }
}

