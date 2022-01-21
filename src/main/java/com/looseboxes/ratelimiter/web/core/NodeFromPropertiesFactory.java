package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateConfigList;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NodeFromPropertiesFactory implements NodeFactory<RateLimitProperties, RateConfigList> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromPropertiesFactory.class);

    @Override
    public Node<NodeData<RateConfigList>> createNode(String name, RateLimitProperties properties) {

        final Node<NodeData<RateConfigList>> rootNode = addNodesToRoot(name, properties.getRateLimitConfigs());

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }

    private Node<NodeData<RateConfigList>> addNodesToRoot(
            String rootNodeName, Map<String, RateConfigList> rateLimitConfigs) {
        Map<String, RateConfigList> configsWithoutParent = new LinkedHashMap<>(rateLimitConfigs);
        RateConfigList rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<RateConfigList> nodeData = rootNodeConfig == null ? null : new NodeData<>(null, rootNodeConfig);
        Node<NodeData<RateConfigList>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        createNodes(rootNode, configsWithoutParent);
        return rootNode;
    }

    public static void createNodes(Node<NodeData<RateConfigList>> parent, Map<String, RateConfigList> rateLimitConfigs) {
        Set<Map.Entry<String, RateConfigList>> entrySet = rateLimitConfigs.entrySet();
        for (Map.Entry<String, RateConfigList> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            NodeUtil.createNode(parent, name, entry.getValue(), entry.getValue());
        }
    }
}
