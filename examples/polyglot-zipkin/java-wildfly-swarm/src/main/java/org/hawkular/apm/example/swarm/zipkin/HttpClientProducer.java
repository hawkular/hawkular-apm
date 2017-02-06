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

package org.hawkular.apm.example.swarm.zipkin;

import java.io.IOException;
import java.net.URI;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientRequestInterceptor;
import com.github.kristofa.brave.ClientResponseInterceptor;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.HttpClientRequest;
import com.github.kristofa.brave.http.HttpClientRequestAdapter;
import com.github.kristofa.brave.http.HttpClientResponseAdapter;
import com.github.kristofa.brave.http.HttpResponse;
import com.github.kristofa.brave.http.SpanNameProvider;

/**
 * @author Pavol Loffay
 */
public class HttpClientProducer {

    private Brave brave;

    @Inject
    public HttpClientProducer(Brave brave) {
        this.brave = brave;
    }

    @Produces
    @Singleton
    public Client getClient() {
        Client client = ClientBuilder.newClient();
        client.register(new BraveClientResponseFilter(brave.clientResponseInterceptor()))
                .register(new BraveClientRequestFilter(new DefaultSpanNameProvider(), brave.clientRequestInterceptor()));

        return client;
    }

    private static class BraveClientRequestFilter implements ClientRequestFilter {

        private final ClientRequestInterceptor requestInterceptor;
        private final SpanNameProvider spanNameProvider;

        public BraveClientRequestFilter(SpanNameProvider spanNameProvider, ClientRequestInterceptor requestInterceptor) {
            this.requestInterceptor = requestInterceptor;
            this.spanNameProvider = spanNameProvider;
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            final HttpClientRequest httpClientRequest = new HttpClientRequest() {
                @Override
                public void addHeader(String s, String s1) {
                    clientRequestContext.getHeaders().add(s, s1);
                }

                @Override
                public URI getUri() {
                    return clientRequestContext.getUri();
                }

                @Override
                public String getHttpMethod() {
                    return clientRequestContext.getMethod();
                }
            };
            requestInterceptor.handle(new HttpClientRequestAdapter(httpClientRequest, spanNameProvider));
        }
    }

    private static class BraveClientResponseFilter implements ClientResponseFilter {

        private final ClientResponseInterceptor responseInterceptor;

        public BraveClientResponseFilter(ClientResponseInterceptor responseInterceptor) {
            this.responseInterceptor = responseInterceptor;
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
            final HttpResponse response = () -> clientResponseContext.getStatus();

            responseInterceptor.handle(new HttpClientResponseAdapter(response));
        }
    }
}
