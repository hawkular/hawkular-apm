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
package org.hawkular.btm.btxn.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.btxn.service.rest.client.BusinessTransactionServiceRESTClient;
import org.junit.Test;

/**
 * These tests invoke a Camel based REST service to cause business transaction information
 * to be reported to the BusinessTransactionService.
 *
 * @author gbrown
 */
public class CamelRESTTest {

    private WebTarget target = ClientBuilder.newClient().target(System.getProperty("hawkular.base-uri")
            + "/camel-example-servlet-rest-tomcat/rest");

    /**  */
    private static final String TEST_PASSWORD = "password";
    /**  */
    private static final String TEST_USERNAME = "jdoe";

    @Test
    public void testInvokeCamelRESTService() {

        // Delay to avoid picking up previously reported txns
        try {
            synchronized (this) {
                wait(500);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        long startTime = System.currentTimeMillis();

        Response resp = target.path("user/123").request().get();

        assertEquals(200, resp.getStatus());

        String user = resp.readEntity(String.class);

        assertTrue("Response should contain user with name 'John Doe'", user.contains("John Doe"));

        // Need to wait for business transaction fragment to be reported to server
        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait");
        }

        // Check if business transaction fragments have been reported
        BusinessTransactionServiceRESTClient service = new BusinessTransactionServiceRESTClient();
        service.setUsername(TEST_USERNAME);
        service.setPassword(TEST_PASSWORD);

        BusinessTransactionCriteria criteria=new BusinessTransactionCriteria().setStartTime(startTime);

        List<BusinessTransaction> btxns = service.query(null, criteria);

        // TODO: Amend once collector capturing business transaction fragments
        assertEquals(0, btxns.size());
    }

}
