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

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.exceptions.SessionManagerException;
import org.onap.cps.spi.exceptions.SessionTimeoutException;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SessionManager {

    private static SessionFactory sessionFactory;
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

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
     * Locks will be released and changes will be committed.
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

    /**
     * Lock Anchor.
     *
     * @param sessionId session ID
     * @param anchorId anchor ID
     * @param timeoutInMilliseconds lock attempt timeout in milliseconds
     */
    public void lockAnchor(final String sessionId, final int anchorId, final Long timeoutInMilliseconds) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final SimpleTimeLimiter timeLimiter = SimpleTimeLimiter.create(executorService);

        try {
            timeLimiter.callWithTimeout(() -> {
                applyPessimisticWriteLockOnAnchor(sessionId, anchorId);
                return null;
            }, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            throw new SessionTimeoutException(
                    "Timeout: Anchor locking prohibited",
                    "The error could be caused by another session holding a lock on the specified table. "
                            + "Retrying the sending the request could be required.");
        }  catch (final ExecutionException | UncheckedExecutionException | InterruptedException caughtException) {
            if (Thread.interrupted()) {
                Thread.currentThread().interrupt();
                throw new SessionManagerException(
                        "Operation interrupted",
                        "This thread was interrupted. ",
                        caughtException
                );
            } else {
                throw new SessionManagerException(
                        "Operation Aborted",
                        "The transaction request was aborted. "
                                + "Retrying and checking all details are correct could be required",
                        caughtException
                );
            }
        } finally {
            log.info("SHUTTING DOWN SERVICE");
            executorService.shutdownNow();
        }
    }

    private void applyPessimisticWriteLockOnAnchor(final String sessionId, final int anchorId) {
        final Session session = sessionMap.get(sessionId);
        final String logMessage = String.format(
                "Attempting to apply lock anchor with anchor ID %s held by session %s", anchorId, sessionId);
        log.info(logMessage);
        session.get(AnchorEntity.class, anchorId, LockMode.PESSIMISTIC_WRITE);
        log.info("Anchor successfully locked");
    }

    /**
     * Release all locks by committing current transaction.
     * A new transaction within the same session will be started for further operations.
     *
     * @param sessionId session ID
     */
    public void releaseLocks(final String sessionId) {
        final Session currentSession = sessionMap.get(sessionId);
        log.info("Attempting to release locks held by session by committing transaction.");
        currentSession.getTransaction().commit();
        currentSession.getTransaction().begin();
        log.info("Locks successfully released. Transaction committed and restarted.");
    }
}