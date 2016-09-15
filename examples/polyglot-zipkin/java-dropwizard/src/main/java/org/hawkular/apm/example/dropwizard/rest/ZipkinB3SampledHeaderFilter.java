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

package org.hawkular.apm.example.dropwizard.rest;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;

/**
 * @author Pavol Loffay
 *
 * Zipkin instrumentation expects header X-B3-Sampled: Boolean (either "1" or "0", can be absent).
 * However, zipkin-tracer (ruby instrumentation) accepts this header as "true". This is causing taht
 * cs,cr and sr,ss spans have different traceId and are not "chained".
 *
 * Issue: https://github.com/openzipkin/zipkin-tracer/issues/68
 * PR solving issue: https://github.com/openzipkin/zipkin-tracer/pull/69
 */
public class ZipkinB3SampledHeaderFilter implements javax.ws.rs.client.ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (requestContext.getUri().getHost().equals(UsersHandler.RUBY_SERVICE_HOST) &&
                requestContext.getHeaders().get("X-B3-Sampled") != null) {
            requestContext.getHeaders().putSingle("X-B3-Sampled", true);
        }
    }
}
