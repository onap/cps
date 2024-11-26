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

package org.onap.cps.ri.utils;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedExecutionException;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.repository.AnchorRepository;
import org.onap.cps.ri.repository.DataspaceRepository;
import org.onap.cps.spi.api.exceptions.SessionManagerException;
import org.onap.cps.spi.api.exceptions.SessionTimeoutException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class SessionManager {

    private final CpsSessionFactory cpsSessionFactory;
    private final TimeLimiterProvider timeLimiterProvider;
    private final DataspaceRepository dataspaceRepository;
    private final AnchorRepository anchorRepository;
    private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
    public static final boolean WITH_COMMIT = true;
    public static final boolean WITH_ROLLBACK = false;

    @PostConstruct
    private void postConstruct() {
        final Thread shutdownHook = new Thread(this::closeAllSessionsInShutdown);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void closeAllSessionsInShutdown() {
        for (final String sessionId : sessionMap.keySet()) {
            try {
                closeSession(sessionId, WITH_ROLLBACK);
                log.info("Session with session ID {} rolled back and closed", sessionId);
            } catch (final Exception e) {
                log.warn("Session with session ID {} failed to close", sessionId);
            }
        }
        cpsSessionFactory.closeSessionFactory();
    }

    /**
     * Starts a session which allows use of locks and batch interaction with the persistence service.
     *
     * @return Session ID string
     */
    public String startSession() {
        final Session session = cpsSessionFactory.openSession();
        final String sessionId = UUID.randomUUID().toString();
        sessionMap.put(sessionId, session);
        session.beginTransaction();
        return sessionId;
    }

    /**
     * Close session.
     * Changes are committed when commit boolean is set to true.
     * Rollback will execute when commit boolean is set to false.
     *
     * @param sessionId session ID
     * @param commit indicator whether session will commit or rollback
     */
    public void closeSession(final String sessionId, final boolean commit) {
        try {
            final Session session = getSession(sessionId);
            if (commit) {
                session.getTransaction().commit();
            } else {
                session.getTransaction().rollback();
            }
            session.close();
        } catch (final HibernateException e) {
            throw new SessionManagerException("Cannot close session",
                String.format("Unable to close session with session ID '%s'", sessionId), e);
        } finally {
            sessionMap.remove(sessionId);
        }
    }

    /**
     * Lock Anchor.
     * To release locks(s), the session holding the lock(s) must be closed.
     *
     * @param sessionId session ID
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param timeoutInMilliseconds lock attempt timeout in milliseconds
     */
    @SneakyThrows
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
                    "Timeout: Anchor locking failed",
                    "The error could be caused by another session holding a lock on the specified table. "
                            + "Retrying the sending the request could be required.", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SessionManagerException("Operation interrupted", "This thread was interrupted.", e);
        } catch (final ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw new SessionManagerException(
                    "Operation Aborted",
                    "The transaction request was aborted. "
                            + "Retrying and checking all details are correct could be required", e);
        } finally {
            executorService.shutdownNow();
        }
    }

    private void applyPessimisticWriteLockOnAnchor(final String sessionId, final String dataspaceName,
                                                   final String anchorName) {
        final Session session = getSession(sessionId);
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final long anchorId = anchorEntity.getId();
        log.debug("Attempting to lock anchor {} for session {}", anchorName, sessionId);
        session.get(AnchorEntity.class, anchorId, LockMode.PESSIMISTIC_WRITE);
        log.info("Anchor {} successfully locked", anchorName);
    }

    private Session getSession(final String sessionId) {
        final Session session = sessionMap.get(sessionId);
        if (session == null) {
            throw new SessionManagerException("Session not found",
                String.format("Session with ID %s does not exist", sessionId));
        }
        return session;
    }

}
