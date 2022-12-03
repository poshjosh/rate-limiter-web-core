package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.rates.Limit;
import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.web.core.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class NodeFromAnnotationsFactory implements NodeFactory<List<Class<?>>, Limit> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromAnnotationsFactory.class);

    private final AnnotationProcessor<Class<?>> annotationProcessor;

    public NodeFromAnnotationsFactory(AnnotationProcessor<Class<?>> annotationProcessor) {
        this.annotationProcessor = Objects.requireNonNull(annotationProcessor);
    }

    @Override
    public Node<NodeData<Limit>> createNode(
            String name,
            List<Class<?>> resourceClasses,
            BiConsumer<Object, Node<NodeData<Limit>>> nodeConsumer) {

        Node<NodeData<Limit>> rootNode = NodeUtil.createNode(name);

        annotationProcessor.process(rootNode, resourceClasses, nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }
}
