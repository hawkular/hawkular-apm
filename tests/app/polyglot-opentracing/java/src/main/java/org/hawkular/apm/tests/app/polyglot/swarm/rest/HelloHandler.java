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

package org.hawkular.apm.tests.app.polyglot.swarm.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.hawkular.apm.tests.app.polyglot.swarm.cdi.APMTracer;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;

/**
 * @author Pavol Loffay
 */
@Path("/")
public class HelloHandler {

    @Inject
    @APMTracer
    private Tracer tracer;

    @GET
    @Path("/hello")
    public Response hello(@Context HttpHeaders headers) {
        SpanContext spanContext =
                tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(Utils.extractHeaders(headers)));

        Span span = tracer.buildSpan("hello")
                .asChildOf(spanContext)
                .start();
        /**
         * Some business logic
         */
        span.close();

        return Response.ok("Hello from WildFly Swarm! [java]").build();
    }

}
