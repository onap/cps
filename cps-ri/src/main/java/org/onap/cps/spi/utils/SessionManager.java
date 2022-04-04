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

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
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
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class SessionManager {

    private final TimeLimiterProvider timeLimiterProvider;
    private final DataspaceRepository dataspaceRepository;
    private final AnchorRepository anchorRepository;
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
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param timeoutInMilliseconds lock attempt timeout in milliseconds
     */
    public void lockAnchor(final String sessionId, final String dataspaceName,
                           final String anchorName, final Long timeoutInMilliseconds) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final TimeLimiter timeLimiter = timeLimiterProvider.getTimeLimiter(executorService);

        try {
            timeLimiter.callWithTimeout(() -> {
                applyPessimisticWriteLockOnAnchor(sessionId, dataspaceName, anchorName);
                return null;
            }, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            throw new SessionTimeoutException(
                    "Timeout: Anchor locking prohibited",
                    "The error could be caused by another session holding a lock on the specified table. "
                            + "Retrying the sending the request could be required.");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SessionManagerException("Operation interrupted", "This thread was interrupted. ", e);
        } catch (final ExecutionException | UncheckedExecutionException e) {
            throw new SessionManagerException(
                    "Operation Aborted",
                    "The transaction request was aborted. "
                            + "Retrying and checking all details are correct could be required", e);
        } finally {
            log.info("SHUTTING DOWN SERVICE");
            executorService.shutdownNow();
        }
    }

    private void applyPessimisticWriteLockOnAnchor(final String sessionId, final String dataspaceName,
                                                   final String anchorName) {
        final Session session = sessionMap.get(sessionId);
        log.info("Getting dataspace entity for dataspace name {}", dataspaceName);
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        log.info("Getting anchor entity for anchorName {}", anchorName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        log.info("Getting anchor ID for anchorName {}", anchorName);
        final int anchorId = anchorEntity.getId();
        log.info("Got anchor ID {}", anchorId);
        log.debug("Attempting to apply lock anchor with anchor ID {} held by session {}", anchorId, sessionId);
        session.get(AnchorEntity.class, anchorId, LockMode.PESSIMISTIC_WRITE);
        log.info("Anchor with ID {} successfully locked", anchorId);
    }

    /**
     * Release all locks by committing current transaction.
     * A new transaction within the same session will be started for further operations.
     *
     * @param sessionId session ID
     */
    public void releaseLocks(final String sessionId) {
        final Session currentSession = sessionMap.get(sessionId);
        log.debug("Attempting to release locks held by session with session ID {} by committing transaction",
                sessionId);
        currentSession.getTransaction().commit();
        currentSession.getTransaction().begin();
        log.info("Locks successfully released. Transaction committed and restarted.");
    }
}