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

package org.hawkular.apm.example.dropwizard.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.hawkular.apm.example.dropwizard.dao.UserDAO;
import org.hawkular.apm.example.dropwizard.model.User;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.BraveExecutorService;

/**
 * @author Pavol Loffay
 */

@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class UsersHandler {

    static final int RUBY_SERVICE_PORT = 3002;
    static final String RUBY_SERVICE_HOST = "ruby";

    private UserDAO userDAO;

    private final Client client;
    private final BraveExecutorService braveExecutorService;

    public UsersHandler(UserDAO userDAO, Client client, Brave brave) {
        this.userDAO = userDAO;
        this.client = Objects.requireNonNull(client);
        this.braveExecutorService = new BraveExecutorService(Executors.newFixedThreadPool(10),
                brave.serverSpanThreadBinder());

        this.client.register(ZipkinB3SampledHeaderFilter.class);
    }

    @GET
    @Path("/users")
    public Response getAll() {
        Collection<User> users = userDAO.getAllUsers();
        return Response.ok().entity(users).build();
    }

    @GET
    @Path("/users/{id}")
    public Response get(@PathParam("id") String id) {
        User user = userDAO.getUser(id);
        return Response.ok().entity(user).build();
    }

    @POST
    @Path("/users")
    public Response create(User user) {
        user = userDAO.createUser(user);
        logToRuby();
        return Response.ok().entity(user).build();
    }

    private void logToRuby() {
        braveExecutorService.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.target("http://" + RUBY_SERVICE_HOST + ":" + RUBY_SERVICE_PORT + "/roda/users").request().get();
        });
    }
}
