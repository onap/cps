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

package org.onap.cps.spi.utils;

import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;


public class SessionManagerHelper {

    private static SessionManagerHelper instance;
    private SessionFactory sessionFactory;
    private ConcurrentHashMap<String, Session> sessionMap;

    private SessionManagerHelper() {
        sessionFactory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(AnchorEntity.class)
                .addAnnotatedClass(DataspaceEntity.class)
                .addAnnotatedClass(SchemaSetEntity.class)
                .addAnnotatedClass(YangResourceEntity.class)
                .buildSessionFactory();
        sessionMap = new ConcurrentHashMap<>();
    }

    /**
     * Get instance of sessionManagerHelper.
     * @return sessionManagerHelper instance
     */
    public static synchronized SessionManagerHelper getInstance() {
        if (instance == null) {
            instance = new SessionManagerHelper();
        }
        return instance;
    }

    public SessionFactory getSessionFactory() {
        return instance.sessionFactory;
    }

    public ConcurrentHashMap<String, Session> getSessionMap() {
        return instance.sessionMap;
    }

}
