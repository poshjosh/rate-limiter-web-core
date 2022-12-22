package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.web.core.NodeBuilder;
import com.looseboxes.ratelimiter.web.core.util.RateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class AnnotationToRateLimitConfigNodeBuilder implements NodeBuilder<List<Class<?>>, RateLimitConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationToRateLimitConfigNodeBuilder.class);

    private final AnnotationProcessor<Class<?>, RateLimitConfig> annotationProcessor;

    public AnnotationToRateLimitConfigNodeBuilder(AnnotationProcessor<Class<?>, RateLimitConfig> annotationProcessor) {
        this.annotationProcessor = Objects.requireNonNull(annotationProcessor);
    }

    @Override
    public Node<NodeData<RateLimitConfig>> buildNode(String name, List<Class<?>> sourceOfRateLimitInfo,
                                                     BiConsumer<Object, Node<NodeData<RateLimitConfig>>> nodeConsumer) {

        Node<NodeData<RateLimitConfig>> rootNode = NodeUtil.createNode(name);

        annotationProcessor.process(rootNode, sourceOfRateLimitInfo, nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }
}
