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
package org.hawkular.apm.tests.common;

import static org.junit.Assert.assertEquals;

import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.Producer;
import org.junit.After;
import org.junit.Before;

/**
 * @author gbrown
 */
public abstract class ClientTestBase {

    private ApmMockServer apmMockServer = new ApmMockServer();

    public int getPort() {
        return 8080;
    }

    @Before
    public void init() {
        try {
            apmMockServer.setPort(getPort());
            apmMockServer.setShutdownTimer(-1); // Disable timer
            apmMockServer.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setProcessHeaders(false);
        setProcessContent(false);
    }

    @After
    public void close() {
        try {
            apmMockServer.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the apmMockServer
     */
    public ApmMockServer getApmMockServer() {
        return apmMockServer;
    }

    /**
     * @param testAPMServer the apmMockServer to set
     */
    public void setApmMockServer(ApmMockServer testAPMServer) {
        this.apmMockServer = testAPMServer;
    }

    /**
     * This method checks that two correlation identifiers are equivalent.
     *
     * @param producer The producer
     * @param consumer The consumer
     */
    protected void checkInteractionCorrelationIdentifiers(Producer producer, Consumer consumer) {
        CorrelationIdentifier pcid = producer.getCorrelationIds().iterator().next();
        CorrelationIdentifier ccid = consumer.getCorrelationIds().iterator().next();

        assertEquals(pcid, ccid);
    }

    protected void setProcessHeaders(boolean b) {
        System.setProperty("hawkular-apm.test.process.headers", ""+b);
    }

    protected void setProcessContent(boolean b) {
        System.setProperty("hawkular-apm.test.process.content", ""+b);
    }

    protected boolean isProcessHeaders() {
        return Boolean.getBoolean("hawkular-apm.test.process.headers");
    }

    protected boolean isProcessContent() {
        return Boolean.getBoolean("hawkular-apm.test.process.content");
    }
}
