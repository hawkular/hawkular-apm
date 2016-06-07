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
package org.hawkular.apm.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.api.services.TraceService;
import org.hawkular.apm.server.api.security.SecurityProvider;
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
@Path("fragments")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "fragments", description = "Report/Query trace fragments")
public class TraceHandler {

    private static final Logger log = Logger.getLogger(TraceHandler.class);

    @Inject
    SecurityProvider securityProvider;

    @Inject
    TraceService traceService;

    @Inject
    TracePublisher tracePublisher;

    @POST
    @ApiOperation(value = "Add a list of trace fragments")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Adding traces succeeded."),
            @ApiResponse(code = 500, message =
                    "Unexpected error happened while storing the trace fragments") })
    public void addTraces(
            @Context SecurityContext context, @HeaderParam("Hawkular-Tenant") String tenantId,
            @Suspended final AsyncResponse response,
            @ApiParam(value = "List of traces", required = true) List<Trace> traces) {

        try {
            tracePublisher.publish(securityProvider.validate(tenantId, context.getUserPrincipal().getName()), traces);

            response.resume(Response.status(Response.Status.OK).build());

        } catch (Throwable t) {
            log.debug(t.getMessage(), t);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + t.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve trace fragment for specified id",
            response = Trace.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, trace fragment found and returned"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Unknown trace fragment id") })
    public void getTrace(
            @Context SecurityContext context, @HeaderParam("Hawkular-Tenant") String tenantId,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true, value = "id of required trace") @PathParam("id") String id) {

        try {
            Trace trace = traceService.get(securityProvider.validate(tenantId, context.getUserPrincipal().getName()), id);

            if (trace == null) {
                log.tracef("Trace fragment '" + id + "' not found");
                response.resume(Response.status(Response.Status.NOT_FOUND).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.tracef("Trace fragment '" + id + "' found");
                response.resume(Response.status(Response.Status.OK).entity(trace).type(APPLICATION_JSON_TYPE)
                        .build());
            }
        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Query trace fragments associated with criteria",
            response = Trace.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void queryTraces(
            @Context SecurityContext context, @HeaderParam("Hawkular-Tenant") String tenantId,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "trace name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "retrieve traces after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "retrieve traces before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "retrieve traces with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                      value = "retrieve traces with these correlation identifiers, defined as a comma "
                                    + "separated list of scope|value "
                                    + "pairs") @DefaultValue("") @QueryParam("correlations") String correlations) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeCorrelationIdentifiers(criteria.getCorrelationIds(), correlations);

            log.tracef("Query trace fragments for criteria [%s]", criteria);

            List<Trace> btxns = traceService.query(securityProvider.validate(tenantId, context.getUserPrincipal().getName()), criteria);

            log.tracef("Queried trace fragments for criteria [%s] = %s", criteria, btxns);

            response.resume(Response.status(Response.Status.OK).entity(btxns).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @POST
    @Path("query")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Query trace fragments associated with criteria",
            response = Trace.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void queryTracesWithCriteria(
            @Context SecurityContext context, @HeaderParam("Hawkular-Tenant") String tenantId,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Query trace fragments for criteria [%s]", criteria);

            List<Trace> btxns = traceService.query(securityProvider.validate(tenantId, context.getUserPrincipal().getName()), criteria);

            log.tracef("Queried trace fragments for criteria [%s] = %s", criteria, btxns);

            response.resume(Response.status(Response.Status.OK).entity(btxns).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @DELETE
    @Path("/")
    @Produces(APPLICATION_JSON)
    public void clear(
            @Context SecurityContext context, @HeaderParam("Hawkular-Tenant") String tenantId,
            @Suspended final AsyncResponse response) {

        try {
            if (System.getProperties().containsKey("hawkular-apm.testmode")) {
                traceService.clear(securityProvider.validate(tenantId, context.getUserPrincipal().getName()));

                response.resume(Response.status(Response.Status.OK).type(APPLICATION_JSON_TYPE)
                        .build());
            } else {
                response.resume(Response.status(Response.Status.FORBIDDEN).type(APPLICATION_JSON_TYPE)
                        .build());
            }

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

}
