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
package org.hawkular.apm.tests.client.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

/**
 * @author gbrown
 */
public class NettyNoResponseHttpITest extends ClientTestBase {

    private static final String HELLO_THERE = "Hello there";
    private static final String QUERY_1 = "name=value";
    private static final String PATH_1 = "/hello";
    private static final String PATH_2 = "/world";
    private static final String PATH_3 = "/space";

    private HttpServer<ByteBuf, ByteBuf> server;

    @Override
    public void init() {
        server = HttpServer.newServer()
                .enableWireLogging(LogLevel.DEBUG)
                .start((req, resp) -> {
                    if (req.getHeader(Constants.HAWKULAR_APM_TRACEID) == null) {
                        return resp.setStatus(HttpResponseStatus.BAD_REQUEST);
                    }
                    if (req.getHttpMethod() == HttpMethod.POST
                            || req.getHttpMethod() == HttpMethod.PUT) {
                        req.getContent().subscribe(bb -> System.out.println("DATA = " + bb.toString()));
                    }
                    resp.setStatus(HttpResponseStatus.OK);
                    return resp;
                }
                );

        super.init();
    }

    @Override
    public void close() {
        server.shutdown();
        server.awaitShutdown();
        super.close();
    }

    @Test
    public void testGET() throws InterruptedException, ExecutionException, TimeoutException {
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", server.getServerPort());

        /*Create a new client for the server address*/
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(serverAddress);
        HttpClientRequest<ByteBuf, ByteBuf> req1 = client.createGet(PATH_1 + "?" + QUERY_1);

        Object result1 = req1
                .flatMap((HttpClientResponse<ByteBuf> resp) -> resp.getContent()
                        .map(bb -> bb.toString(Charset.defaultCharset())))
                .singleOrDefault(null).toBlocking().toFuture().get(5, TimeUnit.SECONDS);

        assertNull(result1);

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(PATH_1, testProducer.getUri());
        assertEquals(QUERY_1, testProducer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
        assertEquals("GET", testProducer.getOperation());
        assertEquals("GET", testProducer.getProperties("http_method").iterator().next().getValue());
    }

    @Test
    public void testPOST() throws InterruptedException, ExecutionException, TimeoutException {
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", server.getServerPort());

        /*Create a new client for the server address*/
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(serverAddress);
        HttpClientRequest<ByteBuf, ByteBuf> req1 = client.createPost(PATH_2);
        req1.writeStringContent(Observable.just(HELLO_THERE));

        Object result1 = req1
                .flatMap((HttpClientResponse<ByteBuf> resp) -> resp.getContent()
                        .map(bb -> bb.toString(Charset.defaultCharset())))
                .singleOrDefault(null).toBlocking().toFuture().get(5, TimeUnit.SECONDS);

        assertNull(result1);

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(PATH_2, testProducer.getUri());
        assertTrue(testProducer.getProperties(Constants.PROP_HTTP_QUERY).isEmpty());
        assertEquals("POST", testProducer.getOperation());
        assertEquals("POST", testProducer.getProperties("http_method").iterator().next().getValue());
    }

    @Test
    public void testPUT() throws InterruptedException, ExecutionException, TimeoutException {
        SocketAddress serverAddress = new InetSocketAddress("127.0.0.1", server.getServerPort());

        /*Create a new client for the server address*/
        HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient(serverAddress);
        HttpClientRequest<ByteBuf, ByteBuf> req1 = client.createPut(PATH_3);
        req1.writeStringContent(Observable.just(HELLO_THERE));

        Object result1 = req1
                .flatMap((HttpClientResponse<ByteBuf> resp) -> resp.getContent()
                        .map(bb -> bb.toString(Charset.defaultCharset())))
                .singleOrDefault(null).toBlocking().toFuture().get(5, TimeUnit.SECONDS);

        assertNull(result1);

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(PATH_3, testProducer.getUri());
        assertTrue(testProducer.getProperties(Constants.PROP_HTTP_QUERY).isEmpty());
        assertEquals("PUT", testProducer.getOperation());
        assertEquals("PUT", testProducer.getProperties("http_method").iterator().next().getValue());
    }
}
