package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.util.CompositeRate;
import com.looseboxes.ratelimiter.annotation.AnnotationTreeBuilder;
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

public class NodeFromAnnotationsFactory implements NodeFactory<List<Class<?>>, CompositeRate> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromAnnotationsFactory.class);

    private final AnnotationTreeBuilder<Class<?>> annotationTreeBuilder;

    public NodeFromAnnotationsFactory(AnnotationTreeBuilder<Class<?>> annotationTreeBuilder) {
        this.annotationTreeBuilder = Objects.requireNonNull(annotationTreeBuilder);
    }

    @Override
    public Node<NodeData<CompositeRate>> createNode(
            String name,
            List<Class<?>> resourceClasses,
            BiConsumer<Object, Node<NodeData<CompositeRate>>> nodeConsumer) {

        Node<NodeData<CompositeRate>> rootNode = NodeUtil.createNode(name);

        annotationTreeBuilder.build(rootNode, resourceClasses, nodeConsumer);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }
}
