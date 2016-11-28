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

package org.hawkular.apm.tests.dist;

import java.io.IOException;
import java.util.List;

import org.hawkular.apm.api.utils.PropertyUtil;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import zipkin.Codec;
import zipkin.Span;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractITest {

    public static final String HAWKULAR_APM_USERNAME = "jdoe";
    public static final String HAWKULAR_APM_PASSWORD = "password";
    public static final String HAWKULAR_APM_URI = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI);
    public static final String HAWKULAR_TENANT_HEADER = "Hawkular-Tenant";
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

    private final ObjectMapper objectMapper;
    private final OkHttpClient client;

    public enum Server {
        APM("/hawkular/apm"),
        Zipkin("/api/v1");

        private String url;
        Server(String basePath) {
            this.url = System.getProperty(PropertyUtil.HAWKULAR_APM_URI) + basePath;
        }

        public String getURL() {
            return url;
        }
    }

    public AbstractITest() {
        this.objectMapper = new ObjectMapper();
        this.client = new OkHttpClient();
    }

    protected Response post(Server server, String path, String tenant, Object payload) throws IOException {
        Request.Builder request = new Request.Builder()
                .post(RequestBody.create(MEDIA_TYPE_JSON, serialize(server, payload)))
                .url(server.getURL() + path);

        if (tenant != null) {
            request.addHeader(HAWKULAR_TENANT_HEADER, tenant);
        }

        Response response = execute(request.build(), server);

        switch (server) {
            case Zipkin: {
                Assert.assertEquals(202, response.code());
            }
        }

        return response;
    }

    private Response execute(Request request, Server server) throws IOException {
        System.out.format("---> Request: %s\n", request);
        Response response = client.newCall(request).execute();
        System.out.format("<--- Response: %s\n", response.toString());

        return response;
    }

    private byte[] serialize(Server server, Object payload) throws JsonProcessingException {
        byte[] json;
        switch (server) {
            case Zipkin:
                json = Codec.JSON.writeSpans((List<Span>) payload);
                break;
            case APM:
            default:
                json = objectMapper.writeValueAsBytes(payload);
        }

        return json;
    }
}
