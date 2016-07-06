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
package org.hawkular.apm.trace.publisher.rest.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.PublisherMetricHandler;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.rest.AbstractRESTClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Trace Publisher
 * API.
 *
 * @author gbrown
 */
public class TracePublisherRESTClient extends AbstractRESTClient implements TracePublisher {

    private static final Logger log = Logger.getLogger(TracePublisherRESTClient.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();

    private PublisherMetricHandler<Trace> handler = null;

    public TracePublisherRESTClient() {
        super(PropertyUtil.HAWKULAR_APM_URI_PUBLISHER);
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.Publisher#getInitialRetryCount()
     */
    @Override
    public int getInitialRetryCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.TracePublisher#publish(java.lang.String, java.util.List)
     */
    @Override
    public void publish(String tenantId, List<Trace> traces) throws Exception {

        URL url = new URL(getUri() + "hawkular/apm/fragments");

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Publish traces [tenant=" + tenantId + "][url=" + url + "]: " + traces);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type",
                "application/json");

        addHeaders(connection, tenantId);

        long startTime = 0;
        if (handler != null) {
            startTime = System.currentTimeMillis();
        }

        java.io.OutputStream os = connection.getOutputStream();

        os.write(mapper.writeValueAsBytes(traces));

        os.flush();
        os.close();

        int statusCode = connection.getResponseCode();

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Status code is: " + statusCode);
        }

        if (handler != null) {
            handler.published(tenantId, traces, (System.currentTimeMillis() - startTime));
        }

        if (statusCode != 200) {
            if (log.isLoggable(Level.FINER)) {
                log.finer("Failed to publish trace fragments: status=[" + statusCode + "]");
            }
            throw new Exception(connection.getResponseMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.Publisher#publish(java.lang.String, java.util.List, int, long)
     */
    @Override
    public void publish(String tenantId, List<Trace> items, int retryCount, long delay)
                            throws Exception {
        throw new java.lang.UnsupportedOperationException("Cannot set the retry count and delay");
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.Publisher#retry(java.lang.String, java.util.List, java.lang.String, int, long)
     */
    @Override
    public void retry(String tenantId, List<Trace> items, String subscriber, int retryCount, long delay)
            throws Exception {
        throw new java.lang.UnsupportedOperationException("Cannot retry");
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.Publisher#setMetricHandler(org.hawkular.apm.api.services.PublisherMetricHandler)
     */
    @Override
    public void setMetricHandler(PublisherMetricHandler<Trace> handler) {
        this.handler = handler;
    }

}
