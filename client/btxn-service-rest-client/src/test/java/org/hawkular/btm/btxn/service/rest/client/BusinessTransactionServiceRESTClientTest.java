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
package org.hawkular.btm.btxn.service.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier.Scope;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.junit.Test;

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

/**
 * @author gbrown
 */
public class BusinessTransactionServiceRESTClientTest {

    @Test
    public void testGetQueryParametersNoCriteria() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);
        assertTrue("Empty map expected", queryParameters.isEmpty());
    }

    @Test
    public void testGetQueryParametersStartTime() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setStartTime(100);

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);
        assertEquals("100", queryParameters.get("startTime"));
    }

    @Test
    public void testGetQueryParametersEndTime() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.setEndTime(200);

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);
        assertEquals("200", queryParameters.get("endTime"));
    }

    @Test
    public void testGetQueryParametersSingleProperties() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.getProperties().put("prop1", "value1");

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertEquals("prop1|value1", queryParameters.get("properties"));
    }

    @Test
    public void testGetQueryParametersMultipleProperties() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();
        criteria.getProperties().put("prop1", "value1");
        criteria.getProperties().put("prop2", "value2");

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("properties"));

        assertTrue(queryParameters.get("properties").equals("prop1|value1,prop2|value2")
                || queryParameters.get("properties").equals("prop2|value2,prop1|value1"));
    }

    @Test
    public void testGetQueryParametersSingleCorrelations() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();

        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Global);
        id1.setValue("value1");

        criteria.getCorrelationIds().add(id1);

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("correlations"));

        assertEquals("Global|value1", queryParameters.get("correlations"));
    }

    @Test
    public void testGetQueryParametersMultipleCorrelations() {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria();

        CorrelationIdentifier id1 = new CorrelationIdentifier();
        id1.setScope(Scope.Global);
        id1.setValue("value1");

        criteria.getCorrelationIds().add(id1);

        CorrelationIdentifier id2 = new CorrelationIdentifier();
        id2.setScope(Scope.Interaction);
        id2.setValue("value2");

        criteria.getCorrelationIds().add(id2);

        Map<String, String> queryParameters = BusinessTransactionServiceRESTClient.getQueryParameters(criteria);

        assertNotNull(queryParameters);

        assertTrue(queryParameters.containsKey("correlations"));

        assertTrue(queryParameters.get("correlations").equals("Global|value1,Interaction|value2")
                || queryParameters.get("correlations").equals("Interaction|value2,Global|value1"));
    }
}
