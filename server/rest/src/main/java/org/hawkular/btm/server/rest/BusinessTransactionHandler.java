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

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionPublisher;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.hawkular.btm.server.api.security.SecurityProvider;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST interface for reporting and querying business transactions.
 *
 * @author gbrown
 *
 */
@Path("fragments")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "fragments", description = "Report/Query business transaction fragments")
public class BusinessTransactionHandler {

    private static final Logger log = Logger.getLogger(BusinessTransactionHandler.class);

    @Inject
    SecurityProvider securityProvider;

    @Inject
    BusinessTransactionService btxnService;

    @Inject
    BusinessTransactionPublisher btxnPublisher;

    @POST
    @ApiOperation(value = "Add a list of business transaction fragments")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Adding business transactions succeeded."),
            @ApiResponse(code = 500, message =
                        "Unexpected error happened while storing the business transaction fragments") })
    public void addBusinessTransactions(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @HeaderParam("tenantId") final String tenantId,
            @ApiParam(value = "List of business transactions", required = true) List<BusinessTransaction> btxns) {

        try {
            btxnPublisher.publish(securityProvider.getTenantId(context), btxns);

            response.resume(Response.status(Response.Status.OK).build());

        } catch (Throwable t) {
            log.debugf(t.getMessage(), t);
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
            value = "Retrieve business transaction fragment for specified id",
            response = BusinessTransaction.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, business transaction fragment found and returned"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Unknown business transaction fragment id") })
    public void getBusinessTransaction(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true, value = "id of required business transaction") @PathParam("id") String id) {

        try {
            BusinessTransaction btxn = btxnService.get(securityProvider.getTenantId(context), id);

            if (btxn == null) {
                log.tracef("Business transaction fragment '" + id + "' not found");
                response.resume(Response.status(Response.Status.NOT_FOUND).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.tracef("Business transaction fragment '" + id + "' found");
                response.resume(Response.status(Response.Status.OK).entity(btxn).type(APPLICATION_JSON_TYPE)
                        .build());
            }
        } catch (Throwable e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Query business transaction fragments associated with criteria",
            response = BusinessTransaction.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void queryBusinessTransactions(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
            value = "business transaction name") @QueryParam("businessTransaction") String businessTransaction,
            @ApiParam(required = false,
                    value = "retrieve business transactions after this time,"
                            + " millisecond since epoch") @DefaultValue("0") @QueryParam("startTime") long startTime,
                    @ApiParam(required = false,
                    value = "retrieve business transactions before this time, "
                            + "millisecond since epoch") @DefaultValue("0") @QueryParam("endTime") long endTime,
                            @ApiParam(required = false,
                    value = "retrieve business transactions with these properties, defined as a comma "
                            + "separated list of name|value "
                            + "pairs") @DefaultValue("") @QueryParam("properties") String properties,
                                    @ApiParam(required = false,
                    value = "retrieve business transactions with these correlation identifiers, defined as a comma "
                            + "separated list of scope|value "
                            + "pairs") @DefaultValue("") @QueryParam("correlations") String correlations) {

        try {
            BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
            criteria.setBusinessTransaction(businessTransaction);
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            RESTServiceUtil.decodeProperties(criteria.getProperties(), properties);

            RESTServiceUtil.decodeCorrelationIdentifiers(criteria.getCorrelationIds(), correlations);

            log.tracef("Query Business transaction fragments for criteria [%s]", criteria);

            List<BusinessTransaction> btxns = btxnService.query(securityProvider.getTenantId(context), criteria);

            log.tracef("Queried Business transaction fragments for criteria [%s] = %s", criteria, btxns);

            response.resume(Response.status(Response.Status.OK).entity(btxns).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debugf(e.getMessage(), e);
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
            value = "Query business transaction fragments associated with criteria",
            response = BusinessTransaction.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void queryBusinessTransactionsWithCriteria(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "query criteria") BusinessTransactionCriteria criteria) {

        try {
            log.tracef("Query Business transaction fragments for criteria [%s]", criteria);

            List<BusinessTransaction> btxns = btxnService.query(securityProvider.getTenantId(context), criteria);

            log.tracef("Queried Business transaction fragments for criteria [%s] = %s", criteria, btxns);

            response.resume(Response.status(Response.Status.OK).entity(btxns).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Throwable e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

}
