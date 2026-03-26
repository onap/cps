/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.dmi.DmiServiceNameResolver;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.JexParser;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadRequestExaminer {

    private static final String DEEP_SEARCH = "//";
    private static final String PATH_SEPARATOR = "/";
    private static final Pattern SPECIFIC_NODE_ID_PATTERN = Pattern.compile(
            "(?:ManagedElement|MeContext)\\[(?:id=|contains\\(id,)");

    private final AlternateIdMatcher alternateIdMatcher;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Classifies selectors from a data node selector string into broadcast, per-DMI, notReady, and error groups.
     *
     * @param dataNodeSelector the selector expression (may contain multiple selectors separated by newline or OR)
     * @return the classified selectors
     */
    public ClassifiedSelectors classifySelectors(final String dataNodeSelector) {
        final List<String> selectors = JexParser.toXpaths(dataNodeSelector);
        final List<String> broadcastSelectors = new ArrayList<>(selectors.size());
        final Map<String, Map<ProducerKey, List<String>>> dmiSelectors = new HashMap<>();
        final List<String> notReadySelectors = new ArrayList<>();
        final List<String> errorSelectors = new ArrayList<>();

        for (final String selector : selectors) {
            if (isBroadcast(selector)) {
                broadcastSelectors.add(selector);
            } else {
                resolveSelector(selector, dmiSelectors, notReadySelectors, errorSelectors);
            }
        }

        return new ClassifiedSelectors(broadcastSelectors, dmiSelectors, notReadySelectors, errorSelectors);
    }

    private void resolveSelector(final String selector,
                                 final Map<String, Map<ProducerKey, List<String>>> dmiSelectors,
                                 final List<String> notReadySelectors,
                                 final List<String> errorSelectors) {
        final Optional<String> searchTerm = JexParser.extractSearchTerm(selector);
        if (searchTerm.isPresent()) {
            resolveSearchTerm(selector, searchTerm.get(), dmiSelectors, notReadySelectors, errorSelectors);
            return;
        }
        final Optional<String> fdnPrefix = JexParser.extractFdnPrefix(selector);
        if (fdnPrefix.isEmpty()) {
            errorSelectors.add(selector);
            return;
        }
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(
                    fdnPrefix.get(), PATH_SEPARATOR);
            addToPerDmiIfReady(selector, cmHandleId, dmiSelectors, notReadySelectors);
        } catch (final NoAlternateIdMatchFoundException exception) {
            errorSelectors.add(selector);
        }
    }

    private void resolveSearchTerm(final String selector,
                                   final String searchTerm,
                                   final Map<String, Map<ProducerKey, List<String>>> dmiSelectors,
                                   final List<String> notReadySelectors,
                                   final List<String> errorSelectors) {
        final Collection<String> cmHandleIds = alternateIdMatcher.getCmHandleIds(searchTerm);
        if (cmHandleIds.isEmpty()) {
            errorSelectors.add(selector);
            return;
        }
        for (final String cmHandleId : cmHandleIds) {
            addToPerDmiIfReady(selector, cmHandleId, dmiSelectors, notReadySelectors);
        }
    }

    private void addToPerDmiIfReady(final String selector,
                                    final String cmHandleId,
                                    final Map<String, Map<ProducerKey, List<String>>> dmiSelectors,
                                    final List<String> notReadySelectors) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        if (isReady(yangModelCmHandle)) {
            final String dmiServiceName = DmiServiceNameResolver.resolveDmiServiceName(
                    RequiredDmiService.DATAJOBS_READ, yangModelCmHandle);
            final ProducerKey producerKey = new ProducerKey(dmiServiceName,
                    yangModelCmHandle.getDataProducerIdentifier());
            dmiSelectors
                    .computeIfAbsent(dmiServiceName, key -> new HashMap<>())
                    .computeIfAbsent(producerKey, key -> new ArrayList<>())
                    .add(selector);
        } else {
            notReadySelectors.add(selector);
        }
    }

    private static boolean isReady(final YangModelCmHandle yangModelCmHandle) {
        return yangModelCmHandle.getCompositeState() != null
                && CmHandleState.READY.equals(yangModelCmHandle.getCompositeState().getCmHandleState());
    }

    private static boolean isBroadcast(final String selector) {
        return selector.contains(DEEP_SEARCH) || !SPECIFIC_NODE_ID_PATTERN.matcher(selector).find();
    }

    public record ClassifiedSelectors(List<String> broadcastSelectors,
                                      Map<String, Map<ProducerKey, List<String>>> dmiSelectors,
                                      List<String> notReadySelectors,
                                      List<String> errorSelectors) { }
}
