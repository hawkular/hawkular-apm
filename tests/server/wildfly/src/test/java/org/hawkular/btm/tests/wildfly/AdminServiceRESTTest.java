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
package org.hawkular.btm.tests.wildfly;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.hawkular.btm.admin.service.rest.client.AdminServiceRESTClient;
import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.junit.Test;

/**
 * @author gbrown
 */
public class AdminServiceRESTTest {

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    @Test
    public void testGetConfig() {
        AdminServiceRESTClient service = new AdminServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        try {
            CollectorConfiguration cc = service.getConfiguration(null, null, null);

            assertNotNull(cc);

            assertNotEquals(0, cc.getInstrumentation().size());

        } catch (Exception e1) {
            fail("Failed to get configuration: " + e1);
        }
    }
}
