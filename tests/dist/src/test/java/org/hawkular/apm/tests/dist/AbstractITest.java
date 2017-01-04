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

package org.hawkular.apm.tests.dist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.hawkular.apm.api.utils.PropertyUtil;
import org.junit.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
        return post(server, path, tenant, payload, Collections.emptyMap());
    }

    protected Response post(Server server, String path, String tenant, Object payload, Map<String, String> headers)
            throws IOException {

        byte[] payloadBytes = serialize(server, payload);
        Map<String, String> caseInsensitiveMap = new TreeMap<>((s1, s2) -> s1.compareToIgnoreCase(s2));
        caseInsensitiveMap.putAll(headers);
        if ("gzip".equalsIgnoreCase(caseInsensitiveMap.get("Content-Encoding"))) {
            payloadBytes = gzipCompression(payloadBytes);
        }

        Request.Builder request = new Request.Builder()
                .post(RequestBody.create(MEDIA_TYPE_JSON, payloadBytes))
                .headers(Headers.of(headers))
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

    public byte[] gzipCompression(byte[] bytes) throws IOException {
        if (bytes == null) {
            return null;
        }

        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);

        gzip.write(bytes);
        gzip.close();
        return obj.toByteArray();
    }
}
