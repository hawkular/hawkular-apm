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
package org.hawkular.apm.client.agent;

import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * @author gbrown
 */
public class ShadeTest {

    @Test
    public void testAgentFound() {
        try {
            Class.forName("org.hawkular.apm.instrumenter.APMAgent");
        } catch (ClassNotFoundException e) {
            fail("Class should have been found");
        }
    }

    @Test(expected=ClassNotFoundException.class)
    public void testJacksonNotFound() throws ClassNotFoundException {
        Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
    }

}
