package com.looseboxes.ratelimiter.web.core;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeData;
import com.looseboxes.ratelimiter.annotation.NodeUtil;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.formatters.NodeFormatters;
import com.looseboxes.ratelimiter.util.RateConfigList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class NodeFromAnnotationsFactory implements NodeFactory<List<Class<?>>, RateConfigList> {

    private static final Logger LOG = LoggerFactory.getLogger(NodeFromAnnotationsFactory.class);

    private final AnnotationProcessor<Class<?>> annotationProcessor;

    public NodeFromAnnotationsFactory(AnnotationProcessor<Class<?>> annotationProcessor) {
        this.annotationProcessor = Objects.requireNonNull(annotationProcessor);
    }

    @Override
    public Node<NodeData<RateConfigList>> createNode(String name, List<Class<?>> resourceClasses) {

        Node<NodeData<RateConfigList>> rootNode = NodeUtil.createNode(name);

        annotationProcessor.process(rootNode, resourceClasses);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatters.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }
}
