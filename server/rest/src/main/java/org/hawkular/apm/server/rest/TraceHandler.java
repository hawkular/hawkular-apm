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
package org.hawkular.apm.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.server.rest.entity.CriteriaRequest;
import org.hawkular.apm.server.rest.entity.GetByIdRequest;
import org.hawkular.apm.server.rest.entity.TenantRequest;
import org.hawkular.jaxrs.filter.tenant.TenantRequired;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST interface for reporting and querying traces.
 *
 * @author gbrown
 *
 */
@Path("traces")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "traces", description = "Report/Query trace fragments")
@TenantRequired(false)
public class TraceHandler extends BaseHandler {
    private static final Logger log = Logger.getLogger(TraceHandler.class);
    TracePublisher tracePublisher;

    @Inject
    TraceService traceService;

    @PostConstruct
    public void init() {
        tracePublisher = ServiceResolver.getSingletonService(TracePublisher.class);

        if (tracePublisher == null) {
            log.error("Unable to locate Trace Publisher");
        }
    }

    @POST
    @Path("fragments")
    @ApiOperation(value = "Add a list of trace fragments")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Adding traces succeeded."),
            @ApiResponse(code = 500, message = "Unexpected error happened while storing the trace fragments") })
    public Response addTraces(@BeanParam TenantRequest request,
                              @ApiParam(value = "List of traces", required = true) List<Trace> traces) {
        return withErrorHandler(() -> {
            tracePublisher.publish(getTenant(request), traces);
            return Response.status(Response.Status.NO_CONTENT).build();
        });
    }

    @GET
    @Path("fragments/{id}")
    @ApiOperation(value = "Retrieve trace fragment for specified id", response = Trace.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, trace found and returned"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Unknown fragment id") })
    public Response getFragment(@BeanParam GetByIdRequest request) {
        return withErrorHandler(() -> {
            Trace trace = traceService.getFragment(getTenant(request), request.getId());

            if (trace == null) {
                log.tracef("Trace fragment [%s] not found", request.getId());
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            } else {
                log.tracef("Trace fragment [%s] found", request.getId());
                return Response
                        .status(Response.Status.OK)
                        .entity(trace)
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }
        });
    }

    @GET
    @Path("complete/{id}")
    @ApiOperation(value = "Retrieve end to end trace for specified id", response = Trace.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, trace found and returned"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Unknown trace id") })
    public Response getTrace(@BeanParam GetByIdRequest request) {
        return withErrorHandler(() -> {
            Trace trace = traceService.getTrace(getTenant(request), request.getId());

            if (trace == null) {
                log.tracef("Trace [%s] not found", request.getId());
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            } else {
                log.tracef("Trace [%s] found", request.getId());
                return Response
                        .status(Response.Status.OK)
                        .entity(trace)
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }
        });
    }

    @GET
    @Path("fragments/search")
    @ApiOperation(value = "Query trace fragments associated with criteria", response = Trace.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response queryFragments(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> traceService.searchFragments(tenant, criteria));
    }

    @DELETE
    @Path("/")
    public Response clear(@BeanParam TenantRequest request) {
        return clearRequest(() -> {
            traceService.clear(getTenant(request));
            return null;
        });
    }
}
