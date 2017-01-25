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
package org.hawkular.apm.tests.agent.opentracing.client.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

import io.opentracing.tag.Tags;

/**
 * @author gbrown
 */
public class ApacheHttpClientITest extends AbstractBaseHttpITest {

    private static final String SAY_HELLO_URL = "http://localhost:8180/sayHello";
    private static final String BAD_URL = "http://localhost:8280/notthere";
    private static final String QUERY_STRING = "to=me";
    private static final String SAY_HELLO_URL_WITH_QS = SAY_HELLO_URL + "?" + QUERY_STRING;
    private static final String SAY_HELLO = "Say Hello";
    private static final String HELLO_WORLD = "Hello World";

    @Test
    public void testHttpClientWithoutResponseHandlerGET() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETBadURL() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(BAD_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETWithQS() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL_WITH_QS), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETNoData() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, false, false);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPUT() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpPut(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPUTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithoutResponseHandler(new HttpPut(SAY_HELLO_URL), SAY_HELLO, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPOST() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpPost(SAY_HELLO_URL), null, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerPOSTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithoutResponseHandler(new HttpPost(SAY_HELLO_URL), SAY_HELLO, false, true);
    }

    @Test
    public void testHttpClientWithoutResponseHandlerGETWithFault() throws IOException {
        testHttpClientWithoutResponseHandler(new HttpGet(SAY_HELLO_URL), null, true, true);
    }

    protected void testHttpClientWithoutResponseHandler(HttpUriRequest request, String data,
            boolean fault, boolean respexpected) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        boolean badUrl = false;
        try {
            request.addHeader("test-header", "test-value");
            if (fault) {
                request.addHeader("test-fault", "true");
            }
            if (!respexpected) {
                request.addHeader("test-no-data", "true");
            }

            if (data != null && request instanceof HttpEntityEnclosingRequest) {
                StringEntity entity = new StringEntity(data,
                        ContentType.create("text/plain", "UTF-8"));
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            }

            HttpResponse response = httpclient.execute(request);

            int status = response.getStatusLine().getStatusCode();

            if (!fault) {
                assertEquals("Unexpected response code", 200, status);

                if (respexpected) {
                    HttpEntity entity = response.getEntity();

                    assertNotNull(entity);

                    assertEquals(HELLO_WORLD, EntityUtils.toString(entity));
                }
            } else {
                assertEquals("Unexpected fault response code", 401, status);
            }

        } catch (ConnectException ce) {
            assertEquals(BAD_URL, request.getURI().toString());
            badUrl = true;
        } finally {
            httpclient.close();
        }

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(request.getURI().getPath(), testProducer.getUri());

        if (!badUrl) {
            if (request.getURI().toString().endsWith(QUERY_STRING)) {
                assertEquals(QUERY_STRING, testProducer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
            }

            if (fault) {
                assertEquals("401", testProducer.getProperties(Tags.HTTP_STATUS.getKey())
                        .iterator().next().getValue());
            }
        } else {
            assertEquals("true", testProducer.getProperties(Tags.ERROR.getKey())
                    .iterator().next().getValue());
        }
    }

    @Test
    public void testHttpClientWithResponseHandlerGET() throws IOException {
        testHttpClientWithResponseHandler(new HttpGet(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerGETBadURL() throws IOException {
        testHttpClientWithResponseHandler(new HttpGet(BAD_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerGETWithQS() throws IOException {
        testHttpClientWithResponseHandler(new HttpGet(SAY_HELLO_URL_WITH_QS), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerGETWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithResponseHandler(new HttpGet(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPUT() throws IOException {
        testHttpClientWithResponseHandler(new HttpPut(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPUTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithResponseHandler(new HttpPut(SAY_HELLO_URL), SAY_HELLO, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPOST() throws IOException {
        testHttpClientWithResponseHandler(new HttpPost(SAY_HELLO_URL), null, false);
    }

    @Test
    public void testHttpClientWithResponseHandlerPOSTWithContent() throws IOException {
        setProcessContent(true);
        testHttpClientWithResponseHandler(new HttpPost(SAY_HELLO_URL), SAY_HELLO, false);
    }

    protected void testHttpClientWithResponseHandler(HttpUriRequest request, String data,
            boolean fault) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        boolean badUrl = false;
        try {
            request.addHeader("test-header", "test-value");
            if (fault) {
                request.addHeader("test-fault", "true");
            }

            if (data != null && request instanceof HttpEntityEnclosingRequest) {
                StringEntity entity = new StringEntity(data,
                        ContentType.create("text/plain", "UTF-8"));
                ((HttpEntityEnclosingRequest) request).setEntity(entity);
            }

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();

                    assertEquals("Unexpected response code", 200, status);

                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                }

            };

            String responseBody = httpclient.execute(request, responseHandler);

            assertEquals(HELLO_WORLD, responseBody);

        } catch (ConnectException ce) {
            assertEquals(BAD_URL, request.getURI().toString());
            badUrl = true;
        } finally {
            httpclient.close();
        }

        Wait.until(() -> getApmMockServer().getTraces().size() == 1);

        // Check stored traces (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());

        List<Producer> producers = NodeUtil.findNodes(getApmMockServer().getTraces().get(0).getNodes(), Producer.class);

        assertEquals("Expecting 1 producers", 1, producers.size());

        Producer testProducer = producers.get(0);

        assertEquals(request.getURI().getPath(), testProducer.getUri());

        if (!badUrl) {
            if (request.getURI().toString().endsWith(QUERY_STRING)) {
                assertEquals(QUERY_STRING, testProducer.getProperties(Constants.PROP_HTTP_QUERY).iterator().next().getValue());
            }
        } else {
            assertEquals("true", testProducer.getProperties(Tags.ERROR.getKey())
                    .iterator().next().getValue());
        }
    }

}
