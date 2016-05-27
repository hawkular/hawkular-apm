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
package org.hawkular.btm.tests.client.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hawkular.btm.api.model.trace.Producer;
import org.hawkular.btm.api.model.trace.Trace;
import org.hawkular.btm.tests.common.ClientTestBase;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

/**
 * @author gbrown
 */
public class NettyHttpTest extends ClientTestBase {

    /**  */
    private static final String HELLO_THERE = "Hello there";
    /**  */
    private static final String QUERY_1 = "name=value";
    /**  */
    private static final String PATH_1 = "/hello";
    /**  */
    private static final String PATH_2 = "/world";
    /**  */
    private static final String PATH_3 = "/space";
    /**  */
    private static final String HELLO_WORLD = "Hello World!";
    private HttpServer<ByteBuf, ByteBuf> server;

    @Override
    public void init() {
        server = HttpServer.newServer(8180)
                .enableWireLogging(LogLevel.DEBUG)
                .start((req, resp) -> {
                    if (req.getHttpMethod() == HttpMethod.POST
                            || req.getHttpMethod() == HttpMethod.PUT) {
                        req.getContent().subscribe(bb -> System.out.println("DATA = " + bb.toString()));
                    }
                    return resp.writeString(Observable.just(HELLO_WORLD));
                }
                        );

        super.init();
    }

    @Override
    public void close() {
        server.shutdown();

        super.close();
    }

    @Test
    public void testGET() {
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 8180);

        /*Create a new client for the server address*/
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(serverAddress);
        HttpClientRequest<ByteBuf, ByteBuf> req1 = client.createGet(PATH_1 + "?" + QUERY_1);

        Object result1 = req1
                .flatMap((HttpClientResponse<ByteBuf> resp) ->
                        resp.getContent()
                                .map(bb -> bb.toString(Charset.defaultCharset()))
                )
                .toBlocking().single();

        assertEquals(HELLO_WORLD, result1);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        for (Trace trace : getTestTraceServer().getTraces()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getTestTraceServer().getTraces().size());

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestTraceServer().getTraces().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(PATH_1, testProducer.getUri());
        assertEquals(QUERY_1, testProducer.getDetails().get("http_query"));
        assertEquals("GET", testProducer.getOperation());
        assertEquals("GET", testProducer.getDetails().get("http_method"));
    }

    @Test
    public void testPOST() {
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 8180);

        /*Create a new client for the server address*/
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(serverAddress);
        HttpClientRequest<ByteBuf, ByteBuf> req1 = client.createPost(PATH_2);
        req1.writeStringContent(Observable.just(HELLO_THERE));

        Object result1 = req1
                .flatMap((HttpClientResponse<ByteBuf> resp) ->
                        resp.getContent()
                                .map(bb -> bb.toString(Charset.defaultCharset()))
                )
                .toBlocking().single();

        assertEquals(HELLO_WORLD, result1);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        for (Trace trace : getTestTraceServer().getTraces()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getTestTraceServer().getTraces().size());

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestTraceServer().getTraces().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(PATH_2, testProducer.getUri());
        assertFalse(testProducer.getDetails().containsKey("http_query"));
        assertEquals("POST", testProducer.getOperation());
        assertEquals("POST", testProducer.getDetails().get("http_method"));
    }

    @Test
    public void testPUT() {
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 8180);

        /*Create a new client for the server address*/
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(serverAddress);
        HttpClientRequest<ByteBuf, ByteBuf> req1 = client.createPut(PATH_3);
        req1.writeStringContent(Observable.just(HELLO_THERE));

        Object result1 = req1
                .flatMap((HttpClientResponse<ByteBuf> resp) ->
                        resp.getContent()
                                .map(bb -> bb.toString(Charset.defaultCharset()))
                )
                .toBlocking().single();

        assertEquals(HELLO_WORLD, result1);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        for (Trace trace : getTestTraceServer().getTraces()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getTestTraceServer().getTraces().size());

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(getTestTraceServer().getTraces().get(0).getNodes(), Producer.class, producers);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(PATH_3, testProducer.getUri());
        assertFalse(testProducer.getDetails().containsKey("http_query"));
        assertEquals("PUT", testProducer.getOperation());
        assertEquals("PUT", testProducer.getDetails().get("http_method"));
    }
}
