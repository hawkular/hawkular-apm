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
import org.hawkular.btm.api.model.analytics.Percentiles;
import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.analytics.Statistics;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.CompletionTimeCriteria;
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
            response = List.class)
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

            java.util.List<URIInfo> uris = analyticsService.getUnboundURIs(
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
    @Path("businesstxn/properties/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the properties used by a business transaction",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getPropertyInfo(
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
            log.tracef("Get property info: name [%s] start [%s] end [%s]", name, startTime, endTime);

            java.util.List<PropertyInfo> pis = analyticsService.getPropertyInfo(
                    securityProvider.getTenantId(context), name, startTime, endTime);

            log.tracef("Got property info: name [%s] start [%s] end [%s] = [%s]", name, startTime, endTime, pis);

            response.resume(Response.status(Response.Status.OK).entity(pis).type(APPLICATION_JSON_TYPE)
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
    @Path("businesstxn/completion/count")
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
            CompletionTimeCriteria criteria = new CompletionTimeCriteria();
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

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/completion/faultcount")
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
            CompletionTimeCriteria criteria = new CompletionTimeCriteria();
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

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/completion/percentiles")
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
            CompletionTimeCriteria criteria = new CompletionTimeCriteria();
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

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("businesstxn/completion/statistics")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionStatistics(
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
            CompletionTimeCriteria criteria = new CompletionTimeCriteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeFaults(criteria.getFaults(), faults);

            log.tracef("Get business transaction completion statistics for criteria [%s] interval [%s]",
                    criteria, interval);

            List<Statistics> stats = analyticsService.getCompletionStatistics(securityProvider.getTenantId(context),
                                        criteria, interval);

            log.tracef("Got business transaction completion statistics for criteria [%s] = %s", criteria, stats);

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

    @POST
    @Path("businesstxn/completion/statistics")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Get the business transaction completion statistics associated with criteria",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCompletionStatistics(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
            value = "aggregation time interval (in milliseconds)") @DefaultValue("60000")
                @QueryParam("interval") long interval,
            @ApiParam(required = true,
            value = "query criteria") CompletionTimeCriteria criteria) {

        try {
            log.tracef("Get business transaction completion statistics for criteria [%s] interval [%s]",
                                    criteria, interval);

            List<Statistics> stats = analyticsService.getCompletionStatistics(securityProvider.getTenantId(context),
                                        criteria, interval);

            log.tracef("Got business transaction completion statistics for criteria [%s] = %s", criteria, stats);

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
    @Path("businesstxn/completion/faults")
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
            CompletionTimeCriteria criteria = new CompletionTimeCriteria();
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

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @POST
    @Path("businesstxn/completion/faults")
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
                value = "query criteria") CompletionTimeCriteria criteria) {

        try {
            log.tracef("Get business transaction completion fault details for criteria (POST) [%s]",
                    criteria);

            List<Cardinality> cards = analyticsService.getCompletionFaultDetails(
                    securityProvider.getTenantId(context), criteria);

            log.tracef("Got business transaction completion fault details for criteria (POST) [%s] = %s",
                                criteria, cards);

            response.resume(Response.status(Response.Status.OK).entity(cards).type(APPLICATION_JSON_TYPE)
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
    @Path("businesstxn/completion/property/{property}")
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
            CompletionTimeCriteria criteria = new CompletionTimeCriteria();
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

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @POST
    @Path("businesstxn/completion/property/{property}")
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
                value = "query criteria") CompletionTimeCriteria criteria) {

        try {
            log.tracef("Get business transaction completion property details for criteria (POST) [%s] property [%s]",
                    criteria, property);

            List<Cardinality> cards = analyticsService.getCompletionPropertyDetails(
                    securityProvider.getTenantId(context), criteria, property);

            log.tracef("Got business transaction completion property details for criteria (POST) [%s] = %s",
                                        criteria, cards);

            response.resume(Response.status(Response.Status.OK).entity(cards).type(APPLICATION_JSON_TYPE)
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
