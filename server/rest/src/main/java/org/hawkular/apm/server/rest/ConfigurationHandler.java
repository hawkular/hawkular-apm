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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.config.txn.TransactionSummary;
import org.hawkular.apm.api.services.ConfigurationService;
import org.hawkular.apm.server.rest.entity.CollectorConfigurationRequest;
import org.hawkular.apm.server.rest.entity.NamedTransactionRequest;
import org.hawkular.apm.server.rest.entity.TenantRequest;
import org.hawkular.apm.server.rest.entity.UpdatedBusinessTxnConfigurationsRequest;
import org.hawkular.jaxrs.filter.tenant.TenantRequired;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST interface for administration capabilities.
 *
 * @author gbrown
 *
 */
@Path("config")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "config", description = "Configuration")
@TenantRequired(false)
public class ConfigurationHandler extends BaseHandler {
    private static final Logger log = Logger.getLogger(ConfigurationHandler.class);

    @Inject
    ConfigurationService configService;

    @GET
    @Path("collector")
    @ApiOperation(value = "Retrieve the collector configuration for the optionally specified host and server", response = CollectorConfiguration.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getCollectorConfiguration(@BeanParam CollectorConfigurationRequest request) {
        return withErrorHandler(() -> {
            log.tracef("Get collector configuration for type [%s] host [%s] server [%s]", request.getType(), request.getHost(), request.getServer());
            CollectorConfiguration config = configService.getCollector(getTenant(request), request.getType(), request.getHost(), request.getServer());
            log.tracef("Got collector configuration for type [%s] host [%s] server [%s] config=[%s]", request.getType(), request.getHost(), request.getServer(), config);

            return Response
                    .status(Response.Status.OK)
                    .entity(config)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("transaction/summary")
    @ApiOperation(value = "Retrieve the transaction summaries", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getBusinessTxnConfigurationSummaries(@BeanParam TenantRequest request) {
        return withErrorHandler(() -> {
            log.tracef("Get transaction summaries");

            List<TransactionSummary> summaries = configService.getTransactionSummaries(getTenant(request));

            // Sort the list
            Collections.sort(summaries, (one, another) -> {
                if (one.getName() == null || another.getName() == null) {
                    return 0;
                }
                return one.getName().compareTo(another.getName());
            });

            log.tracef("Got transaction summaries=[%s]", summaries);

            return Response
                    .status(Response.Status.OK)
                    .entity(summaries)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("transaction/full")
    @ApiOperation(value = "Retrieve the transaction configurations, changed since an optional specified time", response = Map.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getBusinessTxnConfigurations(@BeanParam UpdatedBusinessTxnConfigurationsRequest request) {
        return withErrorHandler(() -> {
            log.tracef("Get transactions, updated = [%s]", request.getUpdated());
            Map<String, TransactionConfig> btxns = configService.getTransactions(getTenant(request), request.getUpdated());
            log.tracef("Got transactions=[%s]", btxns);

            return Response
                    .status(Response.Status.OK)
                    .entity(btxns)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @GET
    @Path("transaction/full/{name}")
    @ApiOperation(value = "Retrieve the transaction configuration for the specified name", response = TransactionConfig.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getBusinessTxnConfiguration(@BeanParam NamedTransactionRequest request) {
        return withErrorHandler(() -> {
            log.tracef("Get transaction configuration for name [%s]", request.getName());
            TransactionConfig config = configService.getTransaction(getTenant(request), request.getName());
            log.tracef("Got transaction configuration for name [%s] config=[%s]", request.getName(), config);

            return Response
                    .status(Response.Status.OK)
                    .entity(config)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    @PUT
    @Path("transaction/full/{name}")
    @ApiOperation(value = "Add or update the transaction configuration for the specified name", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response setBusinessTxnConfiguration(@BeanParam NamedTransactionRequest request, TransactionConfig config) {
        return withErrorHandler(() -> {
            log.tracef("About to set transaction configuration for name [%s] config=[%s]", request.getName(), config);
            List<ConfigMessage> messages = configService.setTransaction(getTenant(request), request.getName(), config);
            log.tracef("Updated transaction configuration for name [%s] messages=[%s]", request.getName(), messages);

            return Response
                    .status(Response.Status.OK)
                    .entity(messages)
                    .build();

        });
    }

    @POST
    @Path("transaction/full")
    @ApiOperation(value = "Add or update the transaction configurations", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response setBusinessTxnConfigurations(@BeanParam TenantRequest request, Map<String, TransactionConfig> btxnConfigs) {
        return withErrorHandler(() -> {
            log.tracef("About to set transaction configurations=[%s]", btxnConfigs);
            List<ConfigMessage> messages = configService.setTransactions(getTenant(request), btxnConfigs);
            log.tracef("Updated transaction configurations : messages=[%s]", messages);

            return Response
                    .status(Response.Status.OK)
                    .entity(messages)
                    .build();
        });
    }

    @DELETE
    @Path("transaction/full/{name}")
    @ApiOperation(value = "Remove the transaction configuration with the specified name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response removeBusinessTxnConfiguration(@BeanParam NamedTransactionRequest request) {
        return withErrorHandler(() -> {
            log.tracef("About to remove transaction configuration for name [%s]", request.getName());
            configService.removeTransaction(getTenant(request), request.getName());
            return Response.status(Response.Status.OK).build();
        });
    }

    @POST
    @Path("transaction/validate")
    @ApiOperation(value = "Validate the transaction configuration", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response validateBusinessTxnConfiguration(@BeanParam TenantRequest request, TransactionConfig config) {
        return withErrorHandler(() -> {
            log.tracef("Validate transaction configuration=[%s]", config);
            List<ConfigMessage> messages = configService.validateTransaction(config);
            log.tracef("Validated transaction configuration: messages=[%s]", messages);

            return Response
                    .status(Response.Status.OK)
                    .entity(messages)
                    .build();
        });
    }

    @DELETE
    @Path("/")
    public Response clear(@BeanParam TenantRequest request) {
        return clearRequest(() -> {
            configService.clear(getTenant(request));
            return null;
        });
    }
}
