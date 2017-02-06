/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.apm.server.infinispan;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Singleton;

import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.hawkular.apm.server.api.utils.zipkin.SpanUniqueIdGenerator;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.jboss.logging.Logger;

/**
 * @author Pavol Loffay
 */
@Singleton
public class InfinispanSpanCache implements SpanCache, ServiceLifecycle {

    private static final Logger log = Logger.getLogger(InfinispanSpanCache.class);

    private static final String SPAN_CACHE = "span";
    private static final String TRACE_CACHE = "spanTrace";
    private static final String CHILDREN_CACHE = "spanChildren";

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer cacheContainer;

    private Cache<String, Span> spansCache;
    /**
     * key is trace and value set of spans which belongs to supplied trace
     */
    private Cache<String, Set<Span>> traceCache;

    private Cache<String, Set<Span>> childrenCache;

    public InfinispanSpanCache() {}

    public InfinispanSpanCache(CacheContainer cacheContainer) {
        this.cacheContainer = cacheContainer;
        init();
    }

    @Override
    @PostConstruct
    public void init() {
        if (spansCache != null && childrenCache != null && traceCache != null) {
            return;
        }

        spansCache = createCache(SPAN_CACHE);
        childrenCache = createCache(CHILDREN_CACHE);
        traceCache = createCache(TRACE_CACHE);
    }

    /**
     * Note that method assumes that span id was changed with {@link SpanUniqueIdGenerator#toUnique(Span)}.
     */
    @Override
    public Span get(String tenantId, String id) {
        Span span = spansCache.get(id);
        log.debugf("Get span [id=%s] = %s", id, span);
        return span;
    }

    @Override
    public void store(String tenantId, List<Span> spans) throws CacheException {
        store(tenantId, spans, x -> x.getId());
    }

    @Override
    public void store(String tenantId, List<Span> spans, Function<Span, String> cacheKeyEntrySupplier)
            throws CacheException {

        if (cacheContainer != null) {
            spansCache.startBatch();
            childrenCache.startBatch();
            traceCache.startBatch();
        }

        for (Span span : spans) {
            log.debugf("Store span [%s]" + span);

            spansCache.put(cacheKeyEntrySupplier.apply(span), span, 1, TimeUnit.MINUTES);

            if (span.getTraceId() != null) {
                synchronized (traceCache) {
                    Set<Span> trace = traceCache.get(span.getTraceId());
                    if (trace == null) {
                        trace = new CopyOnWriteArraySet<>();
                    }
                    trace.add(span);
                    traceCache.put(span.getTraceId(), trace);
                }
            }
            if (span.getParentId() != null && !span.serverSpan()) {
                synchronized (childrenCache) {
                    Set<Span> children = childrenCache.get(span.getParentId());
                    if (children == null) {
                        children = new CopyOnWriteArraySet<>();
                    }
                    children.add(span);
                    childrenCache.put(span.getParentId(), children);
                }
            }
        }

        if (cacheContainer != null) {
            spansCache.endBatch(true);
            childrenCache.endBatch(true);
            traceCache.endBatch(true);

        }
    }

    /**
     * Note that method assumes that id was changed with {@link SpanUniqueIdGenerator#toUnique(Span)}.
     */
    @Override
    public Set<Span> getChildren(String tenant, String id) {
        if (id == null) {
            throw new NullPointerException("Id should not be null!");
        }

        Set<Span> children = childrenCache.get(id);

        return children == null ? null : Collections.unmodifiableSet(children);
    }

    @Override
    public Set<Span> getTrace(String tenant, String id) {
        if (id == null) {
            throw new NullPointerException("Id should not be null!");
        }

        Set<Span> trace = traceCache.get(id);

        return trace == null ? null : Collections.unmodifiableSet(trace);
    }

    private <K, V> Cache<K, V> createCache(String name) {
        Function<String, Cache<K, V>> creator = cacheContainer == null ?
                InfinispanCacheManager::getDefaultCache :
                cacheContainer::getCache;

        log.debugf("Using %s cache manager, for %s cache", cacheContainer == null ? "default" : "container", name);

        return creator.apply(name);
    }
}
