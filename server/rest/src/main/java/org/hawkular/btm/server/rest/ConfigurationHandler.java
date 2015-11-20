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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnSummary;
import org.hawkular.btm.api.model.config.btxn.ConfigMessage;
import org.hawkular.btm.api.services.ConfigurationService;
import org.hawkular.btm.server.api.security.SecurityProvider;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
public class ConfigurationHandler {

    private static final Logger log = Logger.getLogger(ConfigurationHandler.class);

    @Inject
    SecurityProvider securityProvider;

    @Inject
    ConfigurationService configService;

    @GET
    @Path("collector")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve the collector configuration for the optionally specified host and server",
            response = CollectorConfiguration.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getCollectorConfiguration(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
            value = "optional host name") @QueryParam("host") String host,
            @ApiParam(required = false,
            value = "optional server name") @QueryParam("server") String server) {

        try {
            log.tracef("Get collector configuration for host [%s] server [%s]", host, server);

            CollectorConfiguration config = configService.getCollector(
                    securityProvider.getTenantId(context), host, server);

            log.tracef("Got collector configuration for host [%s] server [%s] config=[%s]", host, server, config);

            response.resume(Response.status(Response.Status.OK).entity(config).type(APPLICATION_JSON_TYPE)
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
    @Path("businesstxnsummary")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve the business transaction summaries",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getBusinessTxnConfigurationSummaries(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response) {

        try {
            log.tracef("Get business transaction summaries");

            List<BusinessTxnSummary> summaries = configService.getBusinessTransactionSummaries(
                    securityProvider.getTenantId(context));

            // Sort the list
            Collections.sort(summaries, new Comparator<BusinessTxnSummary>() {
                @Override
                public int compare(BusinessTxnSummary arg0, BusinessTxnSummary arg1) {
                    if (arg0.getName() == null || arg1.getName() == null) {
                        return 0;
                    }
                    return arg0.getName().compareTo(arg1.getName());
                }
            });

            log.tracef("Got business transaction summaries=[%s]", summaries);

            response.resume(Response.status(Response.Status.OK).entity(summaries).type(APPLICATION_JSON_TYPE)
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
    @Path("businesstxn")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve the business transaction configurations, changed since an optional specified time",
            response = Map.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getBusinessTxnConfigurations(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
                    value = "updated since") @QueryParam("updated") @DefaultValue("0") long updated) {

        try {
            log.tracef("Get business transactions, updated = [%s]", updated);

            Map<String, BusinessTxnConfig> btxns = configService.getBusinessTransactions(
                    securityProvider.getTenantId(context), updated);

            log.tracef("Got business transactions=[%s]", btxns);

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

    @GET
    @Path("businesstxn/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve the business transaction configuration for the specified name",
            response = BusinessTxnConfig.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getBusinessTxnConfiguration(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @PathParam("name") String name) {

        try {
            log.tracef("Get business transaction configuration for name [%s]", name);

            BusinessTxnConfig config = configService.getBusinessTransaction(
                    securityProvider.getTenantId(context), name);

            log.tracef("Got business transaction configuration for name [%s] config=[%s]", name, config);

            response.resume(Response.status(Response.Status.OK).entity(config).type(APPLICATION_JSON_TYPE)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).type(APPLICATION_JSON_TYPE).build());
        }
    }

    @PUT
    @Path("businesstxn/{name}")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(
            value = "Add or update the business transaction configuration for the specified name",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void setBusinessTxnConfiguration(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @PathParam("name") String name,
            BusinessTxnConfig config) {

        try {
            log.tracef("About to set business transaction configuration for name [%s] config=[%s]", name,
                    config);

            List<ConfigMessage> messages = configService.updateBusinessTransaction(
                    securityProvider.getTenantId(context), name, config);

            log.tracef("Updated business transaction configuration for name [%s] messages=[%s]", name, messages);

            response.resume(Response.status(Response.Status.OK).entity(messages)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).build());
        }
    }

    @POST
    @Path("businesstxn/validate")
    @Consumes(APPLICATION_JSON)
    @ApiOperation(
            value = "Validate the business transaction configuration",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void validateBusinessTxnConfiguration(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            BusinessTxnConfig config) {

        try {
            log.tracef("Validate business transaction configuration=[%s]", config);

            List<ConfigMessage> messages = configService.validateBusinessTransaction(config);

            log.tracef("Validated business transaction configuration: messages=[%s]", messages);

            response.resume(Response.status(Response.Status.OK).entity(messages)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).build());
        }
    }

    @DELETE
    @Path("businesstxn/{name}")
    @ApiOperation(
            value = "Remove the business transaction configuration with the specified name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void removeBusinessTxnConfiguration(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = true,
            value = "business transaction name") @PathParam("name") String name) {

        try {
            log.tracef("About to remove business transaction configuration for name [%s]", name);

            configService.removeBusinessTransaction(
                    securityProvider.getTenantId(context), name);

            response.resume(Response.status(Response.Status.OK)
                    .build());

        } catch (Exception e) {
            log.debugf(e.getMessage(), e);
            Map<String, String> errors = new HashMap<String, String>();
            errors.put("errorMsg", "Internal Error: " + e.getMessage());
            response.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errors).build());
        }
    }
}
