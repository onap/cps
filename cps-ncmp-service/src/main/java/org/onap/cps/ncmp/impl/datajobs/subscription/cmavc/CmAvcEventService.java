/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.impl.datajobs.subscription.cmavc;

import static org.onap.cps.cpspath.parser.CpsPathUtil.NO_PARENT_PATH;
import static org.onap.cps.cpspath.parser.CpsPathUtil.getNormalizedParentXpath;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.utils.ContentType.JSON;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent;
import org.onap.cps.ncmp.events.avc1_0_0.Edit;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.YangParser;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmAvcEventService {

    private static final OffsetDateTime NO_TIMESTAMP = null;

    private final CpsDataService cpsDataService;
    private final CpsAnchorService cpsAnchorService;
    private final JsonObjectMapper jsonObjectMapper;
    private final YangParser yangParser;

    /**
     * Process the incoming AvcEvent and apply the changes to the cache.
     *
     * @param cmHandleId cm handle identifier
     * @param cmAvcEvent cm avc event
     */
    public void processCmAvcEvent(final String cmHandleId, final AvcEvent cmAvcEvent) {

        final List<Edit> edits =
            cmAvcEvent.getData().getPushChangeUpdate().getDatastoreChanges().getIetfYangPatchYangPatch().getEdit();

        edits.forEach(
            edit -> {
                final String operationNameUpperCase = edit.getOperation().toUpperCase();
                handleCmAvcEventOperation(CmAvcOperationEnum.valueOf(operationNameUpperCase), cmHandleId, edit);
            });
    }

    private void handleCmAvcEventOperation(final CmAvcOperationEnum cmAvcOperation, final String cmHandleId,
                                           final Edit cmAvcEventEdit) {

        log.info("Operation : {} requested for cmHandleId : {}", cmAvcOperation.getValue(), cmHandleId);

        switch (cmAvcOperation) {
            case UPDATE:
                handleUpdate(cmHandleId, cmAvcEventEdit);
                break;
            case PATCH:
                handlePatch(cmHandleId, cmAvcEventEdit);
                break;
            case DELETE:
                handleDelete(cmHandleId, cmAvcEventEdit);
                break;
            default:  // CREATE (checkstyle complains if there is NO default)
                handleCreate(cmHandleId, cmAvcEventEdit);
        }
    }

    private void handleCreate(final String cmHandleId, final Edit cmAvcEventEdit) {
        log.debug("Handling create operation for cmHandleId : {}", cmHandleId);
        final String jsonData = extractNodeData(cmAvcEventEdit);
        cpsDataService.saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, jsonData, NO_TIMESTAMP);
    }

    private void handleUpdate(final String cmHandleId, final Edit cmAvcEventEdit) {
        final String jsonData = extractNodeData(cmAvcEventEdit);
        final String cpsPathFromRestConfStylePath = getCpsPath(cmHandleId, cmAvcEventEdit.getTarget());
        final String parentXpath = getNormalizedParentXpath(cpsPathFromRestConfStylePath);
        log.debug("Handling update operation for cmHandleId : {} , cpsPath : {} and parent-xpath : {}", cmHandleId,
            cpsPathFromRestConfStylePath, parentXpath);
        cpsDataService.updateDataNodeAndDescendants(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
            resolveParentNodeXpath(parentXpath), jsonData, NO_TIMESTAMP, JSON);
    }

    private void handlePatch(final String cmHandleId, final Edit cmAvcEventEdit) {
        final String jsonData = extractNodeData(cmAvcEventEdit);
        final String cpsPathFromRestConfStylePath = getCpsPath(cmHandleId, cmAvcEventEdit.getTarget());
        final String parentXpath = getNormalizedParentXpath(cpsPathFromRestConfStylePath);
        log.debug("Handling patch operation for cmHandleId : {} , cpsPath : {} and parent-xpath : {}", cmHandleId,
            cpsPathFromRestConfStylePath, parentXpath);
        cpsDataService.updateNodeLeaves(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
            resolveParentNodeXpath(parentXpath), jsonData, NO_TIMESTAMP, JSON);

    }

    private void handleDelete(final String cmHandleId, final Edit cmAvcEventEdit) {
        if (NO_PARENT_PATH.equals(cmAvcEventEdit.getTarget()) || cmAvcEventEdit.getTarget() == null) {
            log.debug("Deleting all the entries for cmHandleId : {}", cmHandleId);
            cpsDataService.deleteDataNodes(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, NO_TIMESTAMP);
        } else {
            final String cpsPathFromRestConfStylePath = getCpsPath(cmHandleId, cmAvcEventEdit.getTarget());
            log.debug("Deleting data for xpath : {}", cpsPathFromRestConfStylePath);
            cpsDataService.deleteDataNode(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                cpsPathFromRestConfStylePath, NO_TIMESTAMP);

        }
    }

    private String extractNodeData(final Edit cmAvcEventEdit) {
        return jsonObjectMapper.convertJsonString(jsonObjectMapper.asJsonString(cmAvcEventEdit.getValue()),
            String.class);
    }

    private String getCpsPath(final String cmHandleId, final String restConfStylePath) {
        log.debug("Getting cps path from the rest config path : {}", restConfStylePath);
        final Anchor anchor = cpsAnchorService.getAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
        return yangParser.getCpsPathFromRestConfStylePath(anchor, restConfStylePath);
    }

    private String resolveParentNodeXpath(final String parentNodeXpath) {
        return parentNodeXpath.isEmpty() ? CpsPathUtil.ROOT_NODE_XPATH : parentNodeXpath;
    }

}
