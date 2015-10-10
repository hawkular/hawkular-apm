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
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.services.AdminService;
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
@Path("admin")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "admin", description = "Administration")
public class AdminHandler {

    private static final Logger log = Logger.getLogger(AdminHandler.class);

    @Inject
    SecurityProvider securityProvider;

    @Inject
    AdminService adminService;

    @GET
    @Path("config")
    @Produces(APPLICATION_JSON)
    @ApiOperation(
            value = "Retrieve the collector configuration for the optionally specified host and server",
            response = CollectorConfiguration.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public void getConfiguration(
            @Context SecurityContext context,
            @Suspended final AsyncResponse response,
            @ApiParam(required = false,
            value = "optional host name") @QueryParam("host") String host,
            @ApiParam(required = false,
            value = "optional server name") @QueryParam("server") String server) {

        try {
            log.tracef("Get collector configuration for host [%s] server [%s]", host, server);

            CollectorConfiguration config = adminService.getConfiguration(securityProvider.getTenantId(context),
                    host, server);

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
}
