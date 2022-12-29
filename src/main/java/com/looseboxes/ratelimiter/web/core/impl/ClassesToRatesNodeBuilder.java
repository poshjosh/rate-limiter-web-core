package com.looseboxes.ratelimiter.web.core.impl;

import com.looseboxes.ratelimiter.annotation.AnnotationProcessor;
import com.looseboxes.ratelimiter.annotation.NodeValue;
import com.looseboxes.ratelimiter.node.Node;
import com.looseboxes.ratelimiter.node.NodeFormatter;
import com.looseboxes.ratelimiter.util.Rates;
import com.looseboxes.ratelimiter.web.core.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

final class ClassesToRatesNodeBuilder implements NodeBuilder<List<Class<?>>, Rates> {

    private static final Logger LOG = LoggerFactory.getLogger(ClassesToRatesNodeBuilder.class);

    private final AnnotationProcessor<Class<?>, Rates> annotationProcessor;

    ClassesToRatesNodeBuilder(AnnotationProcessor<Class<?>, Rates> annotationProcessor) {
        this.annotationProcessor = Objects.requireNonNull(annotationProcessor);
    }

    @Override
    public Node<NodeValue<Rates>> buildNode(String name, List<Class<?>> sourceOfRateLimitInfo,
                                            AnnotationProcessor.NodeConsumer<Rates> nodeConsumer) {

        Node<NodeValue<Rates>> rootNode = Node.of(name);

        annotationProcessor.processAll(rootNode, nodeConsumer, sourceOfRateLimitInfo);

        if(LOG.isTraceEnabled()) {
            LOG.trace("Element nodes: {}", NodeFormatter.indentedHeirarchy().format(rootNode));
        }

        return rootNode;
    }
}
