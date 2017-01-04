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

package org.hawkular.apm.example.swarm.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.hawkular.apm.example.swarm.dao.User;
import org.hawkular.apm.example.swarm.dao.UserDAO;

/**
 * @author Pavol Loffay
 */
@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class UserHandler {

    @Inject
    private UserDAO userDAO;

    @Inject
    private Client client;

    @GET
    @Path("/users")
    public Response getAll() {
        Collection<User> users = userDAO.getAllUsers();
        return Response.ok().entity(users).build();
    }

    @GET
    @Path("/users/{id}")
    public Response getOne(@PathParam("id") String id) {
        User user = userDAO.getUser(id);
        return Response.ok().entity(user).build();
    }

    @POST
    @Path("/users")
    public Response getOne(User user) {
        user = userDAO.createUser(user);

        client.target("http://python:3004/pyramid/users").request().get().close();

        return Response.ok().entity(user).build();
    }
}
