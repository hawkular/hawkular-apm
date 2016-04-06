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
package org.hawkular.btm.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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

import org.hawkular.btm.api.model.analytics.Cardinality;
import org.hawkular.btm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.btm.api.model.analytics.CompletionTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.EndpointInfo;
import org.hawkular.btm.api.model.analytics.NodeSummaryStatistics;
import org.hawkular.btm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.Criteria;
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

    private static final java.util.logging.Logger perfLog =
            java.util.logging.Logger.getLogger("org.hawkular.btm.performance.analytics");

    private static final Logger log = Logger.getLogger(AnalyticsHandler.class);

    @Inject
    SecurityProvider securityProvider;

    @Inject
    AnalyticsService analyticsService;

    @GET
    @Path("unboundendpoints")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Identify the unbound endpoints",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getUnboundEndpoints(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
            value = "optional 'start' time, default 1 hour before current time") @DefaultValue("0")
            @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
            value = "optional 'end' time, default current time") @DefaultValue("0") @QueryParam("endTime")
            long endTime,
            @ApiParam(required = false,
            value = "compress list to show common patterns") @DefaultValue("false") @QueryParam("compress")
            boolean compress) {

        try {
            log.tracef("Get unbound endpoints: start [%s] end [%s]", startTime, endTime);

            java.util.List<EndpointInfo> endpoints = analyticsService.getUnboundEndpoints(
                    securityProvider.getTenantId(context), startTime, endTime, compress);

            log.tracef("Got unbound endpoints: start [%s] end [%s] = [%s]", startTime, endTime, endpoints);

            response.resume(Response.status(Response.Status.OK).entity(endpoints).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("boundendpoints/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Identify the bound endpoints for a business transaction",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getBoundEndpoints(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @PathParam("name") String name,
            @ApiParam(required = false,
            value = "optional 'start' time, default 1 hour before current time") @DefaultValue("0")
            @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
            value = "optional 'end' time, default current time") @DefaultValue("0") @QueryParam("endTime")
            long endTime) {

        try {
            log.tracef("Get bound endpoints: name [%s] start [%s] end [%s]", name, startTime, endTime);

            java.util.List<String> endpoints = analyticsService.getBoundEndpoints(
                    securityProvider.getTenantId(context), name, startTime, endTime);

            log.tracef("Got bound endpoints: name [%s] start [%s] end [%s] = [%s]", name, startTime, endTime,
                                                endpoints);

            response.resume(Response.status(Response.Status.OK).entity(endpoints).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("properties")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get property information",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getPropertyInfo(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @QueryParam("businessTransaction")
            String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "faults") @QueryParam("faults") String faults) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get property info for criteria [%s]", criteria);

            java.util.List<PropertyInfo> pis = analyticsService.getPropertyInfo(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got property info for criteria [%s] = [%s]", criteria, pis);

            response.resume(Response.status(Response.Status.OK).entity(pis).type(APPLICATION_JSON_TYPE)
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
    @Path("properties")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get property information",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getPropertyInfo(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get property info for criteria [POST] [%s]", criteria);

            java.util.List<PropertyInfo> pis = analyticsService.getPropertyInfo(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got property info for criteria [POST] [%s] = [%s]", criteria, pis);

            response.resume(Response.status(Response.Status.OK).entity(pis).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("completion/count")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion count",
            response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionCount(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @QueryParam("businessTransaction")
            String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "faults") @QueryParam("faults") String faults) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction count for criteria [%s]", criteria);

            long count = analyticsService.getCompletionCount(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got transaction count: criteria [%s] = [%s]", criteria, count);

            response.resume(Response.status(Response.Status.OK).entity(count).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("completion/faultcount")
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
                    value = "business transaction name") @QueryParam("businessTransaction")
            String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "faults") @QueryParam("faults") String faults) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction fault count for criteria [%s]", criteria);

            long count = analyticsService.getCompletionFaultCount(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got transaction fault count: criteria [%s] = [%s]", criteria, count);

            response.resume(Response.status(Response.Status.OK).entity(count).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("completion/percentiles")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion percentiles associated with criteria",
            response = Percentiles.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionPercentiles(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @QueryParam("businessTransaction")
            String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "faults") @QueryParam("faults") String faults) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction completion percentiles for criteria [%s]", criteria);

            Percentiles stats = analyticsService.getCompletionPercentiles(securityProvider.getTenantId(context),
                    criteria);

            log.tracef("Got business transaction completion percentiles for criteria [%s] = %s", criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("completion/statistics")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion timeseries statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionTimeseriesStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "aggregation time interval (in milliseconds)") @DefaultValue("60000")
            @QueryParam("interval") long interval,
            @ApiParam(required = false,
            value = "faults") @QueryParam("faults") String faults) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction completion timeseries statistics for criteria [%s] interval [%s]",
                    criteria, interval);

            List<CompletionTimeseriesStatistics> stats = analyticsService.getCompletionTimeseriesStatistics(
                    securityProvider.getTenantId(context),
                    criteria, interval);

            log.tracef("Got business transaction completion timeseries statistics for criteria [%s] = %s",
                    criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
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
    @Path("completion/statistics")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion timeseries statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionTimeseriesStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "aggregation time interval (in milliseconds)") @DefaultValue("60000")
            @QueryParam("interval") long interval,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get business transaction completion timeseries statistics for criteria [%s] interval [%s]",
                    criteria, interval);

            List<CompletionTimeseriesStatistics> stats = analyticsService.getCompletionTimeseriesStatistics(
                    securityProvider.getTenantId(context),
                    criteria, interval);

            log.tracef("Got business transaction completion timeseries statistics for criteria [%s] = %s",
                    criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("completion/faults")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion fault details associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionFaultDetails(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "faults") @QueryParam("faults") String faults) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction completion fault details for criteria (GET) [%s]",
                    criteria);

            List<Cardinality> cards = analyticsService.getCompletionFaultDetails(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got business transaction completion fault details for criteria (GET) [%s] = %s",
                    criteria, cards);

            response.resume(Response.status(Response.Status.OK).entity(cards).type(APPLICATION_JSON_TYPE)
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
    @Path("completion/faults")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion fault details associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionFaultDetails(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get business transaction completion fault details for criteria (POST) [%s]",
                    criteria);

            List<Cardinality> cards = analyticsService.getCompletionFaultDetails(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got business transaction completion fault details for criteria (POST) [%s] = %s",
                    criteria, cards);

            response.resume(Response.status(Response.Status.OK).entity(cards).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("completion/property/{property}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion property details associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionPropertyDetails(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "faults") @QueryParam("faults") String faults,
            @ApiParam(required = false,
                                    value = "property") @PathParam("property") String property) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction completion property details for criteria (GET) [%s] property [%s]",
                    criteria, property);

            List<Cardinality> cards = analyticsService.getCompletionPropertyDetails(
                    securityProvider.getTenantId(context), criteria, property);

            log.tracef("Got business transaction completion property details for criteria (GET) [%s] = %s",
                    criteria, cards);

            response.resume(Response.status(Response.Status.OK).entity(cards).type(APPLICATION_JSON_TYPE)
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
    @Path("completion/property/{property}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion property details associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionPropertyDetails(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "property") @PathParam("property") String property,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get business transaction completion property details for criteria (POST) [%s] property [%s]",
                    criteria, property);

            List<Cardinality> cards = analyticsService.getCompletionPropertyDetails(
                    securityProvider.getTenantId(context), criteria, property);

            log.tracef("Got business transaction completion property details for criteria (POST) [%s] = %s",
                    criteria, cards);

            response.resume(Response.status(Response.Status.OK).entity(cards).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
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

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("node/statistics")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction node timeseries statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getNodeTimeseriesStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "host name") @QueryParam("hostName") String hostName,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
            @ApiParam(required = false,
                                    value = "aggregation time interval (in milliseconds)") @DefaultValue("60000")
            @QueryParam("interval") long interval) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);
            criteria.setHostName(hostName);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get business transaction node timeseriesstatistics for criteria [%s] interval [%s]",
                    criteria, interval);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query node timeseries (criteria hash=" + criteria.hashCode()
                        + ")");
            }

            List<NodeTimeseriesStatistics> stats = analyticsService.getNodeTimeseriesStatistics(
                    securityProvider.getTenantId(context),
                    criteria, interval);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query node timeseries (criteria hash=" + criteria.hashCode()
                        + ") duration=" +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got business transaction node timeseries statistics for criteria [%s] = %s", criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
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
    @Path("node/statistics")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction node timeseries statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getNodeTimeseriesStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "aggregation time interval (in milliseconds)") @DefaultValue("60000")
            @QueryParam("interval") long interval,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get business transaction node timeseries statistics for criteria [%s] interval [%s]",
                    criteria, interval);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query node timeseries (criteria hash=" + criteria.hashCode()
                        + ")");
            }

            List<NodeTimeseriesStatistics> stats = analyticsService.getNodeTimeseriesStatistics(
                    securityProvider.getTenantId(context),
                    criteria, interval);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query node timeseries (criteria hash=" + criteria.hashCode()
                        + ") duration=" +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got business transaction node timeseries statistics for criteria [%s] = %s", criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("node/summary")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction node summary statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getNodeSummaryStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "host name") @QueryParam("hostName") String hostName,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);
            criteria.setHostName(hostName);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get business transaction node summary statistics for criteria [%s]",
                    criteria);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query node summary (criteria hash=" + criteria.hashCode() + ")");
            }

            Collection<NodeSummaryStatistics> stats = analyticsService.getNodeSummaryStatistics(
                    securityProvider.getTenantId(context),
                    criteria);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query node summary (criteria hash=" + criteria.hashCode() + ") duration="
                        +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got business transaction node summary statistics for criteria [%s] = %s", criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
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
    @Path("node/summary")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction node summary statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getNodeSummaryStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get business transaction node summary statistics for criteria [%s]",
                    criteria);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query node summary (criteria hash=" + criteria.hashCode() + ")");
            }

            Collection<NodeSummaryStatistics> stats = analyticsService.getNodeSummaryStatistics(
                    securityProvider.getTenantId(context),
                    criteria);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query node summary (criteria hash=" + criteria.hashCode() + ") duration="
                        + (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got business transaction node summary statistics for criteria [%s] = %s", criteria, stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("communication/summary")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction communication summary statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCommunicationSummaryStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "host name") @QueryParam("hostName") String hostName,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);
            criteria.setHostName(hostName);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get business transaction communication summary statistics for criteria [%s]",
                    criteria);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query communication summary (criteria hash="
                        + criteria.hashCode() + ")");
            }

            Collection<CommunicationSummaryStatistics> stats = analyticsService.getCommunicationSummaryStatistics(
                    securityProvider.getTenantId(context),
                    criteria);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query communication summary (criteria hash=" + criteria.hashCode()
                        + ") duration=" +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got business transaction communication summary statistics for criteria [%s] = %s", criteria,
                    stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
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
    @Path("communication/summary")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction communication summary statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCommunicationSummaryStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get business transaction communication summary statistics for criteria [%s]",
                    criteria);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query communication summary (criteria hash="
                        + criteria.hashCode() + ")");
            }

            Collection<CommunicationSummaryStatistics> stats = analyticsService.getCommunicationSummaryStatistics(
                    securityProvider.getTenantId(context),
                    criteria);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query communication summary (criteria hash=" + criteria.hashCode()
                        + ") duration=" +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got business transaction communication summary statistics for criteria [%s] = %s", criteria,
                    stats);

            response.resume(Response.status(Response.Status.OK).entity(stats).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debug(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @GET
    @Path("hostnames")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the host names associated with the criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getHostNames(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
            value = "business transactions after this time,"
                    + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
            @ApiParam(required = false,
                    value = "business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
            @ApiParam(required = false,
                            value = "host name") @QueryParam("hostName") String hostName,
            @ApiParam(required = false,
                            value = "business transactions with these properties, defined as a comma "
                                    + "separated list of name|value "
                                    + "pairs") @DefaultValue("") @QueryParam("properties") String properties) {

        try {
            Criteria criteria = new Criteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);
            criteria.setHostName(hostName);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            log.tracef("Get host names for criteria [%s]",
                    criteria);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query host names (criteria hash=" + criteria.hashCode() + ")");
            }

            List<String> hostnames = analyticsService.getHostNames(
                    securityProvider.getTenantId(context),
                    criteria);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query host names (criteria hash=" + criteria.hashCode() + ") duration=" +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got host names for criteria [%s] = %s", criteria, hostnames);

            response.resume(Response.status(Response.Status.OK).entity(hostnames).type(APPLICATION_JSON_TYPE)
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
    @Path("hostnames")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the host names associated with the criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getHostNames(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
                    value = "query criteria") Criteria criteria) {

        try {
            log.tracef("Get host names for criteria [%s]",
                    criteria);

            long perfStartTime = 0;
            if (perfLog.isLoggable(Level.FINEST)) {
                perfStartTime = System.currentTimeMillis();
                perfLog.finest("Performance: about to query host names (criteria hash=" + criteria.hashCode() + ")");
            }

            List<String> hostnames = analyticsService.getHostNames(
                    securityProvider.getTenantId(context),
                    criteria);

            if (perfLog.isLoggable(Level.FINEST)) {
                perfLog.finest("Performance: query host names (criteria hash=" + criteria.hashCode() + ") duration=" +
                        (System.currentTimeMillis() - perfStartTime) + "ms");
            }

            log.tracef("Got host names for criteria [%s] = %s", criteria, hostnames);

            response.resume(Response.status(Response.Status.OK).entity(hostnames).type(APPLICATION_JSON_TYPE)
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
            @Context SecurityContext context,
            @Suspended final AsyncResponse response) {

        try {
            if (System.getProperties().containsKey("hawkular-btm.testmode")) {
                analyticsService.clear(securityProvider.getTenantId(context));

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
