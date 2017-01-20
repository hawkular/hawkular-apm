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

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.CommunicationSummaryStatistics;
import org.hawkular.apm.api.model.analytics.EndpointInfo;
import org.hawkular.apm.api.model.analytics.NodeTimeseriesStatistics;
import org.hawkular.apm.api.model.analytics.Percentiles;
import org.hawkular.apm.api.model.analytics.TimeseriesStatistics;
import org.hawkular.apm.api.services.AnalyticsService;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.server.rest.entity.BoundEndpointsRequest;
import org.hawkular.apm.server.rest.entity.CriteriaRequest;
import org.hawkular.apm.server.rest.entity.IntervalCriteriaRequest;
import org.hawkular.apm.server.rest.entity.TenantRequest;
import org.hawkular.apm.server.rest.entity.TraceCompletionPropertyRequest;
import org.hawkular.apm.server.rest.entity.TreeCriteriaRequest;
import org.hawkular.apm.server.rest.entity.UnboundEndpointsRequest;
import org.hawkular.jaxrs.filter.tenant.TenantRequired;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST interface for analytics capabilities.
 *
 * @author gbrown
 *
 */
@Path("analytics")
@Produces(APPLICATION_JSON)
@Api(value = "analytics", description = "Analytics")
@TenantRequired(false)
public class AnalyticsHandler extends BaseHandler {
    private static final Logger log = Logger.getLogger(AnalyticsHandler.class);

    @Inject
    AnalyticsService analyticsService;

    @GET
    @Path("unboundendpoints")
    @ApiOperation(value = "Identify the unbound endpoints", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getUnboundEndpoints(@BeanParam UnboundEndpointsRequest request) {
        return withErrorHandler(() -> {
            log.tracef("Get unbound endpoints: start [%s] end [%s]", request.getStartTime(), request.getEndTime());
            List<EndpointInfo> endpoints = analyticsService.getUnboundEndpoints(
                    getTenant(request),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.isCompress()
            );
            log.tracef("Got unbound endpoints: start [%s] end [%s] = [%s]", request.getStartTime(), request.getEndTime(), endpoints);

            return Response
                    .status(Response.Status.OK)
                    .entity(endpoints)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("boundendpoints/{name}")
    @ApiOperation(value = "Identify the bound endpoints for a transaction", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getBoundEndpoints(@BeanParam BoundEndpointsRequest request) {
        return withErrorHandler(() -> {
            log.tracef("Get bound endpoints: name [%s] start [%s] end [%s]", request.getName(), request.getStartTime(), request.getEndTime());
            List<EndpointInfo> endpoints = analyticsService.getBoundEndpoints(
                    getTenant(request),
                    request.getName(),
                    request.getStartTime(),
                    request.getEndTime()
            );
            log.tracef("Got bound endpoints: name [%s] start [%s] end [%s] = [%s]", request.getName(), request.getStartTime(), request.getEndTime(),
                    endpoints);

            return Response
                    .status(Response.Status.OK)
                    .entity(endpoints)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("transactions")
    @ApiOperation(value = "Get transaction information", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTransactionInfo(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getTransactionInfo(tenant, criteria));
    }

    @GET
    @Path("properties")
    @ApiOperation(value = "Get property information", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getPropertyInfo(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getPropertyInfo(tenant, criteria));
    }

    @GET
    @Path("trace/completion/count")
    @ApiOperation(value = "Get the trace completion count", response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionCount(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getTraceCompletionCount(tenant, criteria));
    }

    @GET
    @Path("trace/completion/faultcount")
    @ApiOperation(value = "Get the number of trace instances that returned a fault", response = Long.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionFaultCount(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getTraceCompletionFaultCount(tenant, criteria));
    }

    @GET
    @Path("trace/completion/times")
    @ApiOperation(value = "Get the trace completion times associated with criteria (in descending time order)",
        response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionTimes(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getTraceCompletions(tenant, criteria));
    }

    @GET
    @Path("trace/completion/percentiles")
    @ApiOperation(value = "Get the trace completion percentiles associated with criteria", response = Percentiles.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionPercentiles(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getTraceCompletionPercentiles(tenant, criteria));
    }

    @GET
    @Path("trace/completion/statistics")
    @ApiOperation(value = "Get the trace completion timeseries statistics associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionTimeseriesStatistics(@BeanParam IntervalCriteriaRequest request) {
        return withErrorHandler(() -> {
            Criteria criteria = request.toCriteria();
            log.tracef("Get trace completion timeseries statistics for criteria [%s] interval [%s]", criteria, request.getInterval());
            List<TimeseriesStatistics> stats = analyticsService.getTraceCompletionTimeseriesStatistics(
                    getTenant(request),
                    criteria,
                    request.getInterval()
            );
            log.tracef("Got trace completion timeseries statistics for criteria [%s] = %s", criteria, stats);

            return Response
                    .status(Response.Status.OK)
                    .entity(stats)
                    .type(APPLICATION_JSON_TYPE)
                    .build();

        });
    }

    @GET
    @Path("trace/completion/faults")
    @ApiOperation(value = "Get the trace completion fault details associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionFaultDetails(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getTraceCompletionFaultDetails(tenant, criteria));
    }

    @GET
    @Path("trace/completion/property/{property}")
    @ApiOperation(value = "Get the trace completion property details associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getTraceCompletionPropertyDetails(@BeanParam TraceCompletionPropertyRequest request) {
        return withErrorHandler(() -> {
            Criteria criteria = request.toCriteria();
            log.tracef("Get trace completion property details for criteria (GET) [%s] property [%s]", criteria, request.getProperty());
            List<Cardinality> cards = analyticsService.getTraceCompletionPropertyDetails(getTenant(request), criteria, request.getProperty());
            log.tracef("Got trace completion property details for criteria (GET) [%s] = %s", criteria, cards);

            return Response
                    .status(Response.Status.OK)
                    .entity(cards)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("node/statistics")
    @ApiOperation(value = "Get the trace node timeseries statistics associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getNodeTimeseriesStatistics(@BeanParam IntervalCriteriaRequest request) {
        return withErrorHandler(() -> {
            Criteria criteria = request.toCriteria();
            log.tracef("Get trace node timeseriesstatistics for criteria [%s] interval [%s]", criteria, request.getInterval());
            List<NodeTimeseriesStatistics> stats = analyticsService.getNodeTimeseriesStatistics(getTenant(request), criteria, request.getInterval());
            log.tracef("Got trace node timeseries statistics for criteria [%s] = %s", criteria, stats);

            return Response
                    .status(Response.Status.OK)
                    .entity(stats)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("node/summary")
    @ApiOperation(value = "Get the trace node summary statistics associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getNodeSummaryStatistics(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getNodeSummaryStatistics(tenant, criteria));
    }

    @GET
    @Path("communication/summary")
    @ApiOperation(value = "Get the trace communication summary statistics associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getCommunicationSummaryStatistics(@BeanParam TreeCriteriaRequest request) {
        return withErrorHandler(() -> {
            Criteria criteria = request.toCriteria();
            log.tracef("Get trace communication summary statistics for criteria [%s] as tree [%s]", criteria, request.isTree());
            Collection<CommunicationSummaryStatistics> stats = analyticsService.getCommunicationSummaryStatistics(
                    getTenant(request),
                    criteria,
                    request.isTree()
            );
            log.tracef("Got trace communication summary statistics for criteria [%s] as tree [%s] = %s", criteria, request.isTree(), stats);

            return Response
                    .status(Response.Status.OK)
                    .entity(stats)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("endpoint/response/statistics")
    @ApiOperation(value = "Get the endpoint response timeseries statistics associated with criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getEndpointResponseTimeseriesStatistics(@BeanParam IntervalCriteriaRequest request) {
        return withErrorHandler(() -> {
            Criteria criteria = request.toCriteria();
            log.tracef("Get endpoint response timeseries statistics for criteria [%s] interval [%s]", criteria, request.getInterval());
            List<TimeseriesStatistics> stats = analyticsService.getEndpointResponseTimeseriesStatistics(
                    getTenant(request),
                    criteria,
                    request.getInterval()
            );
            log.tracef("Got endpoint response timeseries statistics for criteria [%s] = %s", criteria, stats);

            return Response
                    .status(Response.Status.OK)
                    .entity(stats)
                    .type(APPLICATION_JSON_TYPE)
                    .build();

        });
    }

    @GET
    @Path("hostnames")
    @ApiOperation(value = "Get the host names associated with the criteria", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getHostNames(@BeanParam CriteriaRequest request) {
        return withCriteria(request, (criteria, tenant) -> analyticsService.getHostNames(tenant, criteria));
    }

    @DELETE
    @Path("/")
    public Response clear(@BeanParam TenantRequest request) {
        return clearRequest(() -> {
            analyticsService.clear(getTenant(request));
            return null;
        });
    }
}
