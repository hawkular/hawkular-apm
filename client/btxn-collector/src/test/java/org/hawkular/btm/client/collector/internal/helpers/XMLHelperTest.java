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
package org.hawkular.btm.client.collector.internal.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.w3c.dom.Node;

/**
 * @author gbrown
 */
public class XMLHelperTest {

    @Test
    public void testEvaluateOrderId() {
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:urn=\"urn:switchyard-quickstart-demo:orders:1.0\">\n   <soapenv:Header/>\n"
                + "<soapenv:Body>\n      <urn:submitOrder>\n         <order>\n            "
                + "<orderId>1</orderId>\n            <itemId>BUTTER</itemId>\n            "
                + "<quantity>100</quantity>\n            <customer>Fred</customer>\n         "
                + "</order>\n      </urn:submitOrder>\n   </soapenv:Body>\n</soapenv:Envelope>";

        String result = XMLHelper.evaluate("*[local-name() = 'Envelope']/*[local-name() = 'Body']"
                + "/*[local-name() = 'submitOrder']/order/orderId/text()", xml);

        assertNotNull(result);
        assertEquals("1", result);
    }

    @Test
    public void testSelectNodeOrder() {
        String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:urn=\"urn:switchyard-quickstart-demo:orders:1.0\">\n   <soapenv:Header/>\n"
                + "<soapenv:Body>\n      <urn:submitOrder>\n         <order>\n            "
                + "<orderId>1</orderId>\n            <itemId>BUTTER</itemId>\n            "
                + "<quantity>100</quantity>\n            <customer>Fred</customer>\n         "
                + "</order>\n      </urn:submitOrder>\n   </soapenv:Body>\n</soapenv:Envelope>";

        Node result = XMLHelper.selectNode("*[local-name() = 'Envelope']/*[local-name() = 'Body']"
                + "/*[local-name() = 'submitOrder']/order", xml);

        assertNotNull(result);
        assertEquals("order", result.getLocalName());
    }

}
