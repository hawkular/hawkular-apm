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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.BraveExecutorService;

/**
 * @author Pavol Loffay
 */
@Path("/")
public class AsyncHandler {

    private final Client client;
    private final BraveExecutorService braveExecutorService;


    public AsyncHandler(Client client, Brave brave) {
        this.client = Objects.requireNonNull(client);
        this.braveExecutorService = new BraveExecutorService(Executors.newFixedThreadPool(10),
                brave.serverSpanThreadBinder());
    }

    @GET
    @Path("/asyncTwoOutgoingCalls")
    public Response asyncTwoOutgoingCalls(@Context HttpServletRequest request) {

        final String pathServiceA = URLUtils.getInServiceURL(request, "/asyncOneOutgoingCall");
        final String pathServiceB = URLUtils.getInServiceURL(request, "/asyncNoOutgoingCall");

        braveExecutorService.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.target(pathServiceA).request().get();
            client.target(pathServiceB).request().get();
        });

        return Response.ok().build();
    }

    @GET
    @Path("/asyncOneOutgoingCall")
    public Response asyncOneOutgoingCall(@Context HttpServletRequest request) throws InterruptedException {

        final String pathService = URLUtils.getInServiceURL(request, "/asyncNoOutgoingCall");

        braveExecutorService.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            client.target(pathService).request().get();
        });

        return Response.ok().build();
    }

    @GET
    @Path("/asyncNoOutgoingCall")
    public void asyncNoOutgoingCall(@Context HttpServletRequest request,
                                    @Suspended final AsyncResponse asyncResponse) throws InterruptedException {

        braveExecutorService.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            asyncResponse.resume("");
        });
    }
}
