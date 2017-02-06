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

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.model.analytics.ServiceDeployment;
import org.hawkular.apm.api.services.ServiceDeploymentService;
import org.hawkular.apm.server.rest.entity.CriteriaRequest;
import org.hawkular.jaxrs.filter.tenant.TenantRequired;
import org.jboss.logging.Logger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * JAX-RS handler for Service Deployments. A service deployment is a special tag, that allows to
 * distinguish between deployments within a specific service. For now, a service deployment is equivalent to a
 * buildStamp, which follows the pattern "{service}-{serialNumber}". In a future iteration, it might mean a set of
 * specific buildStamps acting together among services.
 *
 * @author Juraci Paixão Kröhling
 */
@Path("services")
@Produces(APPLICATION_JSON)
@Api(value = "services", description = "Service Deployments")
@TenantRequired(false)
public class ServiceDeploymentHandler extends BaseHandler {
    private static final Logger log = Logger.getLogger(ServiceDeploymentHandler.class);

    @Inject
    ServiceDeploymentService serviceDeploymentService;

    /**
     * Returns the available services. Service here, at this point, are individual services. A service has 1+ buildStamps.
     *
     * @see ServiceDeploymentService#getService(String, String, org.hawkular.apm.api.services.Criteria)
     * @return a {@link List} of {@link ServiceDeployment}, wrapped into a {@link Response}.
     */
    @GET
    @Path("/")
    @ApiOperation(value = "Returns all the available services", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getServices(@BeanParam CriteriaRequest criteriaRequest) {
        return withErrorHandler(() -> {
            List<ServiceDeployment> serviceDeployments = serviceDeploymentService.getServiceDeployments(
                    getTenant(criteriaRequest),
                    criteriaRequest.toCriteria()
            );
            log.debugf("Returning %d service deployments.", serviceDeployments.size());

            return Response
                    .ok(serviceDeployments)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }

    /**
     * Returns the requested service, including all the build stamps related to it.
     * @param service the service name. Required.
     * @return a {@link ServiceDeployment} for the requested service
     */
    @GET
    @Path("{service}")
    @ApiOperation(value = "Returns all available build stamps for the given service", response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid Service Name"),
            @ApiResponse(code = 404, message = "Service Name not found"),
            @ApiResponse(code = 500, message = "Internal server error") })
    public Response getService(@PathParam("service") String service, @BeanParam CriteriaRequest criteriaRequest) {
        return withErrorHandler(() -> {
            if (null == service || service.isEmpty()) {
                log.debug("Received an bad request: invalid service name.");
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Service Name.").build();
            }

            ServiceDeployment serviceDeployment = serviceDeploymentService.getService(
                    service,
                    getTenant(criteriaRequest),
                    criteriaRequest.toCriteria()
            );

            if (null == serviceDeployment) {
                return Response
                        .status(Response.Status.NOT_FOUND)
                        .entity(String.format("Could not found the service %s", service))
                        .build();
            }

            log.debugf("Returning a service with %d build stamps", serviceDeployment.getBuildStamps().size());
            return Response
                    .ok(serviceDeployment)
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        });
    }
}
