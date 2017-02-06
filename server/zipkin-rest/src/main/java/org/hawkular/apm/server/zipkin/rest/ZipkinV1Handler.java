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
package org.hawkular.apm.server.zipkin.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hawkular.apm.server.api.services.SpanPublisher;
import org.hawkular.apm.server.api.utils.zipkin.ZipkinSpanConvertor;
import org.jboss.logging.Logger;

import zipkin.Codec;

/**
 * REST interface for reporting zipkin spans.
 *
 * @author gbrown
 * @author Pavol Loffay
 */
@Path("v1")
@Produces(APPLICATION_JSON)
public class ZipkinV1Handler {

    private static final Logger log = Logger.getLogger(ZipkinV1Handler.class);

    private static final String APPLICATION_THRIFT = "application/x-thrift";

    @Inject
    private SpanPublisher spanPublisher;


    @POST
    @Path("spans")
    @Consumes(APPLICATION_JSON)
    public Response addJsonSpans(@HeaderParam("Content-Encoding") String encoding, byte[] spans) {
        return acceptSpans(encoding, Codec.JSON, spans);
    }

    @POST
    @Path("spans")
    @Consumes(APPLICATION_THRIFT)
    public Response addThriftSpans(@HeaderParam("Content-Encoding") String encoding, byte[] spans) {
        return acceptSpans(encoding, Codec.THRIFT, spans);
    }

    private Response acceptSpans(String encoding, Codec codec, byte[] body) {
        List<zipkin.Span> spans;
        try {
            spans = codec.readSpans(body);
        } catch (RuntimeException e) {
            log.error("Could not deserialize", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Cannot deserialize spans: " + e.getMessage() + "\n")
                    .build();
        }

        try {
            spanPublisher.publish(null, ZipkinSpanConvertor.spans(spans));
        } catch (Exception e) {
            log.error("Could not publish spans to JMS", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Cannot publish spans tp JMS: " + e.getMessage() + "\n")
                    .build();
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }
}
