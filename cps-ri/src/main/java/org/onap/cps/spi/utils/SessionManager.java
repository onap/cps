/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.springframework.stereotype.Component;

@Component
public class SessionManager {

    private static SessionFactory sessionFactory;
    private static Map<String, Session> sessionMap = new HashMap<>();

    private synchronized void buildSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = new Configuration().configure("hibernate.cfg.xml")
                    .addAnnotatedClass(AnchorEntity.class)
                    .addAnnotatedClass(DataspaceEntity.class)
                    .addAnnotatedClass(SchemaSetEntity.class)
                    .addAnnotatedClass(YangResourceEntity.class)
                    .buildSessionFactory();
        }
    }

    /**
     * Starts a session which allows use of locks and batch interaction with the persistence service.
     *
     * @return Session ID string
     */
    public String startSession() {
        buildSessionFactory();
        final Session session = sessionFactory.openSession();
        final String sessionId = UUID.randomUUID().toString();
        sessionMap.put(sessionId, session);
        session.beginTransaction();
        return sessionId;
    }

    /**
     * Close session.
     *
     * @param sessionId session ID
     */
    public void closeSession(final String sessionId) {
        try {
            final Session currentSession = sessionMap.get(sessionId);
            currentSession.getTransaction().commit();
            currentSession.close();
        } catch (final NullPointerException e) {
            throw new SessionException(String.format("Session with session ID %s does not exist", sessionId));
        } catch (final HibernateException e) {
            throw new SessionException(String.format("Unable to close session with session ID %s", sessionId));
        }
        sessionMap.remove(sessionId);
    }

}