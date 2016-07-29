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
package org.hawkular.apm.tests.tools.instrumenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.tests.common.ClientTestBase;
import org.hawkular.apm.tools.instrumenter.InstrumenterUtil;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class EchoAppTest extends ClientTestBase {

    /**  */
    private static final String HELLO = "hello";

    @Test
    public void testEcho() {
        //System.setProperty("org.jboss.byteman.verbose", "");
        //System.setProperty("HAWKULAR_APM_LOG_LEVEL", "FINEST");
        System.setProperty("HAWKULAR_APM_URI", "http://localhost:8080");

        java.io.File f = new java.io.File("src/test/resources/instrumentation/apmconfig/jvm/hawkular-apm-config.json");

        assertTrue(f.exists());

        ObjectMapper mapper = new ObjectMapper();

        // Need to load the same instrumentation rules (collector configuration) used
        // during the build phase
        CollectorConfiguration config = null;

        try {
            config = mapper.readValue(f, CollectorConfiguration.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to get config");
        }

        assertNotNull(config);

        // Currently the instrumentation is performed in two separate stages, the first
        // at build time to introduce the hooks into the application code, and the second
        // is performed at runtime, to initialise the instrumentation rules (which may be
        // compiled or interpreted).
        InstrumenterUtil.initRuntime(config);

        // Now when running the echo app, the instrumentation rules modify the return result
        // so that it no longer returns the supplied value
        EchoApp app = new EchoApp();

        String result = app.echo(HELLO);

        assertEquals(HELLO, result);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for traces to store");
        }

        for (Trace trace : getApmMockServer().getTraces()) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("TRACE=" + mapper.writeValueAsString(trace));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // Check stored business transactions (including 1 for the test client)
        assertEquals(1, getApmMockServer().getTraces().size());
    }

}
