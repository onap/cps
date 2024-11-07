package org.onap.cps.api;

import java.util.Collection;
import java.util.Map;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.utils.ContentType;

public interface DataNodeBuilderService {

    Collection<DataNode> buildDataNodesWithAnchorAndXpathToNodeData(final Anchor anchor,
            final Map<String, String> nodesData, final ContentType contentType);

    Collection<DataNode> buildDataNodesWithAnchorXpathAndNodeData(final Anchor anchor, final String xpath,
            final String nodeData, final ContentType contentType);

    Collection<DataNode> buildDataNodesWithAnchorParentXpathAndNodeData(final Anchor anchor, final String parentNodeXpath,
            final String nodeData, final ContentType contentType);

    Collection<DataNode> buildDataNodesWithYangResourceXpathAndNodeData(
            final Map<String, String> yangResourcesNameToContentMap, final String xpath,
            final String nodeData, final ContentType contentType);

}
