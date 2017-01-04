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
package org.hawkular.apm.trace.publisher.rest.client;

import java.time.Clock;
import java.util.List;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.PublisherMetricHandler;
import org.hawkular.apm.api.services.TracePublisher;
import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.apm.client.api.rest.AbstractRESTClient;

/**
 * This class provides the REST client implementation for the Trace Publisher
 * API.
 *
 * @author gbrown
 */
public class TracePublisherRESTClient extends AbstractRESTClient implements TracePublisher {
    private static final Logger log = Logger.getLogger(TracePublisherRESTClient.class.getName());
    private PublisherMetricHandler<Trace> handler = null;
    private Clock clock = Clock.systemUTC();

    public TracePublisherRESTClient() {
        super(PropertyUtil.HAWKULAR_APM_URI_PUBLISHER);
    }

    public TracePublisherRESTClient(String username, String password, String url) {
        super(username, password, url);
    }

    @Override
    public int getInitialRetryCount() {
        return 0;
    }

    @Override
    public void publish(String tenantId, List<Trace> traces) throws Exception {
        long startTime = clock.millis();
        int statusCode = postAsJsonTo(tenantId, "traces/fragments", traces);
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Status code is: " + statusCode);
        }

        if (handler != null) {
            handler.published(tenantId, traces, (clock.millis() - startTime));
        }

        if (statusCode != 204) {
            if (log.isLoggable(Level.FINER)) {
                log.finer("Failed to publish trace fragments: status=[" + statusCode + "]");
            }
            throw new Exception("Failed to publish trace fragments: status=[" + statusCode + "]");
        }
    }

    @Override
    public void publish(String tenantId, List<Trace> items, int retryCount, long delay) throws Exception {
        throw new UnsupportedOperationException("Cannot set the retry count and delay");
    }

    @Override
    public void retry(String tenantId, List<Trace> items, String subscriber, int retryCount, long delay) throws Exception {
        throw new UnsupportedOperationException("Cannot retry");
    }

    @Override
    public void setMetricHandler(PublisherMetricHandler<Trace> handler) {
        this.handler = handler;
    }

}
