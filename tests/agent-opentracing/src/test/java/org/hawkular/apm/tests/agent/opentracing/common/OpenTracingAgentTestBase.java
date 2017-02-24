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
package org.hawkular.apm.tests.agent.opentracing.common;

import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;

import io.opentracing.contrib.global.GlobalTracer;
import io.opentracing.mock.MockTracer;

/**
 * @author gbrown
 */
public abstract class OpenTracingAgentTestBase {

    private static MockTracer tracer = new MockTracer();

    @BeforeClass
    public static void init() {
        GlobalTracer.register(tracer);
    }

    @Before
    public void resetTracer() {
        tracer.reset();
    }

    public MockTracer getTracer() {
        return tracer;
    }

    protected String toHttpURL(URL url) {
        if (url.getQuery() != null) {
            // Remove the query string
            StringBuilder urlstring = new StringBuilder(url.toString());
            urlstring.delete(urlstring.length()-url.getQuery().length()-1, urlstring.length());
            return urlstring.toString();
        }
        return url.toString();
    }

}
