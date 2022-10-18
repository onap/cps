package org.onap.cps.utils;

import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SimplifiedModelNode {

    String containerName = null;
    String prefix = null;

    final Map<String, SimplifiedModelNode> children = new HashMap<>();

    public SimplifiedModelNode(final String containerName, final String prefix) {
        this.containerName = containerName;
        this.prefix = prefix;
    }

    public void addChild(SimplifiedModelNode simplifiedModelNode) {
        this.children.put(simplifiedModelNode.containerName, simplifiedModelNode);
    }

    public String resolve(final String ancestorPath, final String descendantsPath) {
        final int indexOfSlash = descendantsPath.indexOf('/');
        final boolean hasSlash = indexOfSlash >= 0;
        final String childName = hasSlash ? descendantsPath.substring(0, indexOfSlash) : descendantsPath;
        SimplifiedModelNode childSimplifiedModelNode = children.get(childName);
        if (childSimplifiedModelNode == null) {
            throw new IllegalArgumentException();
        }
        final boolean insertPrefix = !childSimplifiedModelNode.prefix.equals(prefix);
        final String childPath;
        if (insertPrefix) {
            childPath = ancestorPath + "/" + childSimplifiedModelNode.prefix + ":" + childName;
        } else {
            childPath = ancestorPath + "/" + childName;
        }
        if (hasSlash) {
            return childSimplifiedModelNode.resolve(childPath, descendantsPath.substring(indexOfSlash + 1));
        }
        return childPath;
    }

}
