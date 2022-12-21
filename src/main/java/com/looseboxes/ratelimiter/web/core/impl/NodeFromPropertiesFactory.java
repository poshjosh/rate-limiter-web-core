package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.BandwidthFactory;
import com.looseboxes.ratelimiter.bandwidths.Bandwidth;
import com.looseboxes.ratelimiter.bandwidths.Bandwidths;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.Operator;
import com.looseboxes.ratelimiter.web.core.NodeFactory;
import com.looseboxes.ratelimiter.web.core.util.RateConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;
import com.looseboxes.ratelimiter.web.core.util.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

public class NodeFromPropertiesFactory implements NodeFactory<RateLimitProperties, Bandwidths> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromPropertiesFactory.class);

    public NodeFromPropertiesFactory() { }

    @Override
    public Node<NodeData<Bandwidths>> createNode(
            String name,
            RateLimitProperties properties,
            BiConsumer<Object, Node<NodeData<Bandwidths>>> nodeConsumer) {

        final Node<NodeData<Bandwidths>> rootNode = addNodesToRoot(name, toBandwidths(properties), nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }

    private Map<String, Bandwidths> toBandwidths(RateLimitProperties properties) {
        Map<String, RateLimitConfig> configs = properties.getRateLimitConfigs();
        return toBandwidths(configs);
    }

    private Map<String, Bandwidths> toBandwidths(Map<String, RateLimitConfig> configs) {
        Map<String, Bandwidths> limits = new HashMap<>(configs.size() * 4/3);
        for(String name : configs.keySet()) {
            limits.put(name, toBandwidths(configs.get(name)));
        }
        return limits;
    }

    private Bandwidths toBandwidths(RateLimitConfig rateLimitConfig) {
        final List<RateConfig> limits = rateLimitConfig.getLimits();
        final Operator operator = rateLimitConfig.getLogic();
        if(limits == null || limits.isEmpty()) {
            return Bandwidths.empty(operator);
        }else if(limits.size() == 1) {
            return Bandwidths.of(operator, toBandwidth(limits.get(0)));
        }else {
            return Bandwidths.of(operator, limits.stream().map(this::toBandwidth).toArray(Bandwidth[]::new));
        }
    }

    private Bandwidth toBandwidth(RateConfig config) {
        BandwidthFactory bandwidthFactory = BandwidthFactory.getOrCreateBandwidthFactory(config.getFactoryClass());
        return bandwidthFactory.createNew(config.getLimit(), config.getDuration());
    }

    private Node<NodeData<Bandwidths>> addNodesToRoot(
            String rootNodeName,
            Map<String, Bandwidths> limits,
            BiConsumer<Object, Node<NodeData<Bandwidths>>> nodeConsumer) {
        Map<String, Bandwidths> configsWithoutParent = new LinkedHashMap<>(limits);
        Bandwidths rootNodeConfig = configsWithoutParent.remove(rootNodeName);
        NodeData<Bandwidths> nodeData = rootNodeConfig == null ? null : new NodeData<>(rootNodeConfig, rootNodeConfig);
        Node<NodeData<Bandwidths>> rootNode = NodeUtil.createNode(rootNodeName, nodeData, null);
        nodeConsumer.accept(rootNodeConfig, rootNode);
        createNodes(rootNode, configsWithoutParent, nodeConsumer);
        return rootNode;
    }

    private void createNodes(
            Node<NodeData<Bandwidths>> parent,
            Map<String, Bandwidths> limits,
            BiConsumer<Object, Node<NodeData<Bandwidths>>> nodeConsumer) {
        Set<Map.Entry<String, Bandwidths>> entrySet = limits.entrySet();
        for (Map.Entry<String, Bandwidths> entry : entrySet) {
            String name = entry.getKey();
            if(name.equals(parent.getName())) {
                throw new IllegalStateException("Parent and child nodes both have the same name: " + name);
            }
            Bandwidths nodeConfig = entry.getValue();
            Node<NodeData<Bandwidths>> node = NodeUtil.createNode(parent, name, nodeConfig, nodeConfig);
            nodeConsumer.accept(nodeConfig, node);
        }
    }
}
