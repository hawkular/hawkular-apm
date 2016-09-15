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

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Pavol Loffay
 */
@Path("/")
public class SyncHandler {

    private final Client client;

    private ThreadLocal<Random> randomThreadLocal = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };


    public SyncHandler(Client client) {
        this.client = Objects.requireNonNull(client);
    }

    @GET
    @Path("/A")
    public Response getA(@Context  HttpServletRequest request) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(randomThreadLocal.get().nextInt(1000));

        client.target(URLUtils.getInServiceURL(request, "/AB")).request().get();
        client.target(URLUtils.getInServiceURL(request, "/AC")).request().get();

        return Response.ok().build();
    }

    @GET
    @Path("/AB")
    public Response getAB(@Context HttpServletRequest request) throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(randomThreadLocal.get().nextInt(1000));

        client.target(URLUtils.getInServiceURL(request, "/ABD")).request().get();

        return Response.ok().entity("AB").build();
    }

    @GET
    @Path("/AC")
    public Response getAC() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(randomThreadLocal.get().nextInt(1000));
        return Response.ok().entity("AC").build();
    }

    @GET
    @Path("/ABD")
    public Response getABD() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(randomThreadLocal.get().nextInt(1000));
        return Response.ok().entity("ABD").build();
    }
}
