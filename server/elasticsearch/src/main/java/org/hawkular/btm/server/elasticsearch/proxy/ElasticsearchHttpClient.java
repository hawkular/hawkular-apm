/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.server.elasticsearch.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

/**
 * The ElasticSearch HTTP client.
 *
 * Based on the http servlet proxy implemented by David Smiley:
 * https://github.com/dsmiley/HTTP-Proxy-Servlet
 *
 */
public class ElasticsearchHttpClient {

    private static final String DEFAULT_ELASTIC_SEARCH_URL = "http://localhost:9200";

    private static final Logger LOG = Logger.getLogger(ElasticsearchHttpClient.class.getName());

    private HttpClient proxyClient;
    private String url;

    /**
     * The default constructor.
     */
    public ElasticsearchHttpClient() {
        HttpParams hcParams = new BasicHttpParams();
        proxyClient = new DefaultHttpClient(new PoolingClientConnectionManager(), hcParams);

        // Get URL
        url = System.getProperty("elasticsearch.server", DEFAULT_ELASTIC_SEARCH_URL);
    }

    /**
     * This method processes the supplied HTTP request.
     *
     * @param request The request
     * @return The response
     * @throws Exception Failed to process the request
     */
    public HttpResponse process(HttpServletRequest request) throws Exception {
        HttpRequest proxyRequest;

        String proxyRequestUri = rewriteUrlFromRequest(request);

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Rewritten URL: " + proxyRequestUri);
        }

        //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
        if (request.getHeader(HttpHeaders.CONTENT_LENGTH) != null
                || request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            HttpEntityEnclosingRequest eProxyRequest = new BasicHttpEntityEnclosingRequest(
                    request.getMethod(), proxyRequestUri);

            // Transfer content to byte array
            java.io.InputStream is = request.getInputStream();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            while (true) {
                byte[] b = new byte[10240];
                int len = is.read(b);

                if (len == -1) {
                    break;
                }

                baos.write(b, 0, len);
            }

            is.close();
            baos.close();

            HttpEntity entity = new ByteArrayEntity(baos.toByteArray());

            is.close();

            eProxyRequest.setEntity(entity);
            proxyRequest = eProxyRequest;

        } else {
            proxyRequest = new BasicHttpRequest(request.getMethod(), proxyRequestUri);
        }

        copyRequestHeaders(request, proxyRequest, proxyRequestUri);

        try {
            // Execute the request
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("proxy " + request.getMethod() + " uri: " + request.getRequestURI() + " -- "
                        + proxyRequest.getRequestLine().getUri());
            }

            return (proxyClient.execute(URIUtils.extractHost(
                    new java.net.URI(proxyRequestUri)), proxyRequest));

        } catch (Exception e) {
            //abort request, according to best practice with HttpClient
            if (proxyRequest instanceof AbortableHttpRequest) {
                AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest) proxyRequest;
                abortableHttpRequest.abort();
            }

            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        if (proxyClient != null) {
            proxyClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     *
     * @param request The client request
     * @param proxyRequest The request being sent to the target service
     * @param uri The target service URI
     * @throws Exception Failed to copy headers
     */
    protected void copyRequestHeaders(HttpServletRequest request, HttpRequest proxyRequest,
            String uri) throws Exception {
        java.util.Enumeration<String> iter = request.getHeaderNames();

        while (iter.hasMoreElements()) {
            String key = iter.nextElement();

            //Instead the content-length is effectively set via HttpEntity
            if (key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }

            if (HOPBYHOPHEADERS.containsHeader(key)) {
                continue;
            }

            // In case the proxy host is running multiple virtual servers,
            // rewrite the Host header to ensure that we get content from
            // the correct virtual server

            String headerValue = request.getHeader(key);

            if (key.equalsIgnoreCase(HttpHeaders.HOST)) {
                HttpHost host = URIUtils.extractHost(new java.net.URI(uri));
                headerValue = host.getHostName();
                if (host.getPort() != -1) {
                    headerValue += ":" + host.getPort();
                }
            }

            proxyRequest.addHeader(key, headerValue);
        }
    }

    /** Reads the request URI from {@code servletRequest} and rewrites it, considering {@link
     * #targetUri}. It's used to make the new request.
     */
    protected String rewriteUrlFromRequest(HttpServletRequest request) {
        StringBuilder uri = new StringBuilder(500);

        uri.append(url);

        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            // Basic support for filtering based on user - just to ensure they have different
            // (partitioned) custom dashboards. When fine grained authentication supported, might
            // want to include this as part of a more general mechanism.
            if (pathInfo.startsWith("/kibana-int/dashboard/")) {
                pathInfo = pathInfo.replaceFirst("dashboard", "dashboard-" + request.getUserPrincipal().getName());
            }

            // Append path
            uri.append(pathInfo);
        }

        // Handle the query string
        java.util.Enumeration<String> iter = request.getParameterNames();
        boolean f_first = true;

        if (iter.hasMoreElements()) {
            uri.append('?');
        }

        while (iter.hasMoreElements()) {
            String name = iter.nextElement();

            String[] values = request.getParameterValues(name);

            for (int i = 0; i < values.length; i++) {
                if (!f_first) {
                    uri.append('&');
                }

                uri.append(name);
                uri.append('=');
                uri.append(values[i]);

                f_first = false;
            }
        }

        String ret = uri.toString();

        ret = ret.replaceAll(" ", "%20");

        return (ret);
    }

    /** These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     * I use an HttpClient HeaderGroup class instead of Set<String> because this
     * approach does case insensitive lookup faster.
     */
    private static final HeaderGroup HOPBYHOPHEADERS;

    static {
        HOPBYHOPHEADERS = new HeaderGroup();
        String[] headers = new String[] {
                "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization",
                "TE", "Trailers", "Transfer-Encoding", "Upgrade" };
        for (String header : headers) {
            HOPBYHOPHEADERS.addHeader(new BasicHeader(header, null));
        }
    }

}
