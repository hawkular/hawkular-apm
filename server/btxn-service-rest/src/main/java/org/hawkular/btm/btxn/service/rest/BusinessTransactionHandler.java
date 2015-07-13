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
package org.hawkular.btm.btxn.service.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

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
import javax.ws.rs.core.Response;

import org.hawkular.accounts.api.model.Persona;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.jboss.logging.Logger;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * REST interface for reporting and querying business transactions.
 *
 * @author gbrown
 *
 */
@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/", description = "Report & Query Business Transactions")
public class BusinessTransactionHandler {

    private static final Logger log = Logger.getLogger(BusinessTransactionHandler.class);

    @Inject
    Persona currentPersona;

    @Inject
    BusinessTransactionService btxnService;

    @POST
    @Path("/transactions")
    @ApiOperation(value = "Add a list of business transactions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Adding business transactions succeeded."),
            @ApiResponse(code = 500, message = "Unexpected error happened while storing the business transactions") })
    public void addBusinessTransactions(
            @Suspended final AsyncResponse response,
            @HeaderParam("tenantId") final String tenantId,
            @ApiParam(value = "List of business transactions", required = true) List<BusinessTransaction> btxns) {

        try {
            btxnService.store(currentPersona.getId(), btxns);

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
    @Path("/transactions/{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve business transaction fragment for specified id",
            response = BusinessTransaction.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, business transaction found and returned"),
            @ApiResponse(code = 500, message = "Internal server error"),
            @ApiResponse(code = 404, message = "Unknown business transaction id") })
    public void getBusinessTransaction(@Suspended final AsyncResponse response,
            @ApiParam(required = true, value = "id of required business transaction") @PathParam("id") String id) {

        try {
            BusinessTransaction btxn = btxnService.get(currentPersona.getId(), id);

            if (btxn == null) {
                log.tracef("Business transaction '" + id + "' not found");
                response.resume(Response.status(Response.Status.NOT_FOUND).type(APPLICATION_JSON_TYPE).build());
            } else {
                log.tracef("Business transaction '" + id + "' found");
                response.resume(Response.status(Response.Status.OK).entity(btxn).type(APPLICATION_JSON_TYPE)
                        .build());
            }
        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    @GET
    @Path("/transactions")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Query business transaction fragments associated with criteria",
            response = BusinessTransaction.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void queryBusinessTransactions(
            @Suspended final AsyncResponse response,
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
            criteria.setStartTime(startTime);
            criteria.setEndTime(endTime);

            decodeProperties(criteria.getProperties(), properties);

            decodeCorrelationIdentifiers(criteria.getCorrelationIds(), correlations);

            log.tracef("Query Business transactions for criteria [%s]", criteria);

            List<BusinessTransaction> btxns = btxnService.query(currentPersona.getId(), criteria);

            log.tracef("Queried Business transactions for criteria [%s] = %s", criteria, btxns);

            response.resume(Response.status(Response.Status.OK).entity(btxns).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }

    }

    /**
     * This method processes a comma separated list of properties, defined as a name|value pair.
     *
     * @param properties The properties map
     * @param encoded The string containing the encoded properties
     */
    protected static void decodeProperties(Map<String, String> properties, String encoded) {
        if (encoded != null && encoded.trim().length() > 0) {
            StringTokenizer st = new StringTokenizer(encoded, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                String[] parts = token.split("[|]");
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    String value = parts[1].trim();

                    log.tracef("Extracted property name [%s] value [%s]", name, value);

                    properties.put(name, value);
                }
            }
        }
    }

    /**
     * This method processes a comma separated list of correlation identifiers, defined as a scope|value pair.
     *
     * @param correlations The correlation identifier set
     * @param encoded The string containing the encoded correlation identifiers
     */
    protected static void decodeCorrelationIdentifiers(Set<CorrelationIdentifier> correlations, String encoded) {
        if (encoded != null && encoded.trim().length() > 0) {
            StringTokenizer st = new StringTokenizer(encoded, ",");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                String[] parts = token.split("[|]");
                if (parts.length == 2) {
                    String scope = parts[0].trim();
                    String value = parts[1].trim();

                    log.tracef("Extracted correlation identifier scope [%s] value [%s]", scope, value);

                    CorrelationIdentifier cid = new CorrelationIdentifier();
                    cid.setScope(Scope.valueOf(scope));
                    cid.setValue(value);

                    correlations.add(cid);
                }
            }
        }
    }
}
