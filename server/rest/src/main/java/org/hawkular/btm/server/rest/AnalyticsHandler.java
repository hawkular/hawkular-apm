/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hawkular.btm.api.model.analytics.BusinessTransactionStats;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.server.api.security.SecurityProvider;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST interface for analytics capabilities.
 *
 * @author gbrown
 *
 */
@Path("analytics")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "analytics", description = "Analytics")
public class AnalyticsHandler {

    private static final Logger log = Logger.getLogger(AnalyticsHandler.class);

    @Inject
    SecurityProvider securityProvider;

    @Inject
    AnalyticsService analyticsService;

    @GET
    @Path("businesstxn/unbounduris")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Identify the unbound URIs",
            response = Map.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getUnboundURIs(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "optional 'start' time, default 1 hour before current time")
                        @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "optional 'end' time, default current time") @DefaultValue("0")
                        @QueryParam("endTime") long endTime) {

        try {
            log.tracef("Get unbound URIs: start [%s] end [%s]", startTime, endTime);

            java.util.Map<String,java.util.List<String>> uris = analyticsService.getUnboundURIs(
                    securityProvider.getTenantId(context), startTime, endTime);

            log.tracef("Got unbound URIs: start [%s] end [%s] = [%s]", startTime, endTime, uris);

            response.resume(Response.status(Response.Status.OK).entity(uris).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/bounduris/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Identify the bound URIs for a business transaction",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getBoundURIs(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @PathParam("name") String name,
            @ApiParam(required = false,
                    value = "optional 'start' time, default 1 hour before current time")
                        @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "optional 'end' time, default current time") @DefaultValue("0")
                        @QueryParam("endTime") long endTime) {

        try {
            log.tracef("Get bound URIs: name [%s] start [%s] end [%s]", name, startTime, endTime);

            java.util.List<String> uris = analyticsService.getBoundURIs(
                    securityProvider.getTenantId(context), name, startTime, endTime);

            log.tracef("Got bound URIs: name [%s] start [%s] end [%s] = [%s]", name, startTime, endTime, uris);

            response.resume(Response.status(Response.Status.OK).entity(uris).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/count")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction count",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionCount(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @QueryParam("name") String name,
            @ApiParam(required = false,
                    value = "business transactions after this time,"
                            + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
                    @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
                            @ApiParam(required = false,
                    value = "business transactions with these properties, defined as a comma "
                            + "separated list of name|value "
                            + "pairs") @DefaultValue("") @QueryParam("properties") String properties) {

        try {
            BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
            criteria.setName(name);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            BusinessTransactionHandler.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get business transaction count for criteria [%s]", criteria);

            long count = analyticsService.getCompletionCount(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got transaction count: criteria [%s] = [%s]", criteria, count);

            response.resume(Response.status(Response.Status.OK).entity(count).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/faultcount")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the number of business transaction instances that returned a fault",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionFaultCount(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @QueryParam("name") String name,
            @ApiParam(required = false,
                    value = "business transactions after this time,"
                            + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
                    @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
                            @ApiParam(required = false,
                    value = "business transactions with these properties, defined as a comma "
                            + "separated list of name|value "
                            + "pairs") @DefaultValue("") @QueryParam("properties") String properties) {

        try {
            BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
            criteria.setName(name);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            BusinessTransactionHandler.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get business transaction fault count for criteria [%s]", criteria);

            long count = analyticsService.getCompletionFaultCount(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got transaction fault count: criteria [%s] = [%s]", criteria, count);

            response.resume(Response.status(Response.Status.OK).entity(count).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/stats")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction stats associated with criteria",
            response = BusinessTransactionStats.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionStats(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @QueryParam("name") String name,
            @ApiParam(required = false,
                    value = "business transactions after this time,"
                            + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
                    @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
                            @ApiParam(required = false,
                    value = "business transactions with these properties, defined as a comma "
                            + "separated list of name|value "
                            + "pairs") @DefaultValue("") @QueryParam("properties") String properties) {

        try {
            BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
            criteria.setName(name);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            BusinessTransactionHandler.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get business transaction stats for criteria [%s]", criteria);

            BusinessTransactionStats stats = analyticsService.getCompletionStats(securityProvider.getTenantId(context),
                                        criteria);

            log.tracef("Got business transaction stats for criteria [%s] = %s", criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("alerts/count/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction alert count",
            response = Integer.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getAlertCount(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @PathParam("name") String name) {

        try {
            log.tracef("Get alert count: name [%s]", name);

            int count = analyticsService.getAlertCount(
                    securityProvider.getTenantId(context), name);

            log.tracef("Got alert count: name [%s] = [%s]", name, count);

            response.resume(Response.status(Response.Status.OK).entity(count).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

}
