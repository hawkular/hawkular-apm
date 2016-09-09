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
package org.hawkular.apm.server.zipkin.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hawkular.apm.server.api.model.zipkin.Span;
import org.hawkular.apm.server.api.services.SpanPublisher;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * REST interface for reporting zipkin spans.
 *
 * @author gbrown
 *
 */
@Path("v1")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ZipkinV1Handler {

    private static final Logger log = Logger.getLogger(ZipkinV1Handler.class);

    private static ObjectMapper mapper = new ObjectMapper();

    @Inject
    private SpanPublisher spanPublisher;

    @POST
    @Path("spans")
    public Response addSpans(List<Span> spans) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Span = " + mapper.writeValueAsString(spans));
            }

            spanPublisher.publish(null, spans);

            return Response
                    .status(Response.Status.ACCEPTED)
                    .build();

        } catch (Throwable t) {
            log.errorf(t.getMessage(), t);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("errorMsg", "Internal Error: " + t.getMessage()))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
    }

}
