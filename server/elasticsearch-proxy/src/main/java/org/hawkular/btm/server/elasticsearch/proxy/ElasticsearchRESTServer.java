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

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

/**
 * This class implements the authenticating proxy for the ElasticSearch server.
 *
 * Based on the http servlet proxy implemented by David Smiley:
 * https://github.com/dsmiley/HTTP-Proxy-Servlet
 */
public class ElasticsearchRESTServer extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(ElasticsearchRESTServer.class.getName());

    private ElasticsearchHttpClient client = new ElasticsearchHttpClient();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
            throws IOException {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Elasticsearch Proxy: Request method=" + servletRequest.getMethod() + " path="
                    + servletRequest.getPathInfo());
        }

        if (isSupported(servletRequest)) {
            try {
                HttpResponse resp = client.process(servletRequest);

                servletResponse.setStatus(resp.getStatusLine().getStatusCode());

                copyResponseHeaders(resp, servletResponse);

                // Send the content to the client
                copyResponseContent(resp, servletResponse);

                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Elasticsearch Proxy: Response status code=" + servletResponse.getStatus());
                }

            } catch (ConnectException ce) {
                if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, "Elasticsearch Proxy: connection failure", ce);
                }

                // Return "Service Unavailable" status code
                servletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            } catch (IOException ioe) {
                if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, "Elasticsearch Proxy: I/O failure", ioe);
                }

                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
            } catch (Exception e) {
                if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, "Elasticsearch Proxy: general failure", e);
                }

                servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        } else {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Elasticsearch Proxy: Forbidden request method=" + servletRequest.getMethod() + " path="
                        + servletRequest.getPathInfo());
            }

            // Return "Forbidden" status code
            servletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * This method determines whether the supplied request is supported by this gateway.
     *
     * @param servletRequest The REST request
     * @return Whether the request is supported
     */
    protected boolean isSupported(HttpServletRequest servletRequest) {
        if (servletRequest.getMethod().equalsIgnoreCase("get")) {
            return (true);
        } else if (servletRequest.getMethod().equalsIgnoreCase("post")) {
            if (servletRequest.getPathInfo().endsWith("/_search")) {
                return (true);
            }
        } else if (servletRequest.getMethod().equalsIgnoreCase("put")) {
            if (servletRequest.getPathInfo().startsWith("/kibana-int/dashboard")) {
                return (true);
            }
        }
        return (false);
    }

    /**
     * Copy proxied response headers back to the servlet client.
     *
     * @param proxyResponse The response from the target server
     * @param servletResponse The response back to the client
     */
    protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletResponse servletResponse) {

        for (int i = 0; i < proxyResponse.getAllHeaders().length; i++) {
            Header header = proxyResponse.getAllHeaders()[i];

            if (HOPBYHOPHEADERS.containsHeader(header.getName())) {
                continue;
            }

            servletResponse.addHeader(header.getName(), header.getValue());
        }
    }

    /**
     * Copy response body data (the entity) from the proxy to the servlet client.
     *
     * @param proxyResponse The response from the target server
     * @param servletResponse The response back to the client
     * @throws IOException Failed to copy content
     */
    protected void copyResponseContent(HttpResponse proxyResponse, HttpServletResponse servletResponse)
            throws IOException {
        OutputStream servletOutputStream = servletResponse.getOutputStream();

        try {
            proxyResponse.getEntity().writeTo(servletOutputStream);

        } finally {
            try {
                servletOutputStream.close();
            } catch (Exception e) {
                log(e.getMessage(), e);
            }
        }
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
