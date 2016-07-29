/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.hawkular.apm.api.services.ServiceLifecycle;
import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.CacheException;
import org.hawkular.apm.server.api.services.SpanCache;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

/**
 * @author Pavol Loffay
 */
public class InfinispanSpanCache implements SpanCache, ServiceLifecycle {

    private static final Logger log = Logger.getLogger(InfinispanSpanCache.class.getName());

    protected static final String CACHE_NAME = "span";

    @Resource(lookup = "java:jboss/infinispan/APM")
    private CacheContainer cacheContainer;

    private Cache<String, Span> spansCache;


    @Override
    @PostConstruct
    public void init() {
        // If cache container not already provisions, then must be running outside of a JEE
        // environment, so create a default cache container
        if (cacheContainer == null) {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using default cache");
            }
            setSpansCache(InfinispanCacheManager.getDefaultCache(CACHE_NAME));
        } else {
            if (log.isLoggable(Level.FINER)) {
                log.fine("Using container provided cache");
            }
            setSpansCache(cacheContainer.getCache(CACHE_NAME));
        }

    }

    @Override
    public Span get(String tenantId, String id) {
        Span span = spansCache.get(id);

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Get communication details [id=" + id + "] = " + span);
        }

        return span;
    }

    @Override
    public void store(String tenantId, List<Span> spans) throws CacheException {
        if (cacheContainer != null) {
            spansCache.startBatch();
        }

        for (Span span: spans) {

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Store communication details [id=" + span.getId() + "]: " + span);
            }

            spansCache.put(span.getId(), span, 1, TimeUnit.MINUTES);
        }

        if (cacheContainer != null) {
            spansCache.endBatch(true);
        }
    }

    @Override
    public List<Span> getChildren(String tenant, String parentId) {
        QueryFactory<?> queryFactory = Search.getQueryFactory(spansCache);
        Query query = queryFactory.from(Span.class)
                .having("parentId")
                .eq(parentId)
                .toBuilder().build();

        List<Span> queryResult = query.list();

        return queryResult;
    }

    public Cache<String, Span> getSpansCache() {
        return spansCache;
    }

    public void setSpansCache(Cache<String, Span> spansCache) {
        this.spansCache = spansCache;
    }
}
