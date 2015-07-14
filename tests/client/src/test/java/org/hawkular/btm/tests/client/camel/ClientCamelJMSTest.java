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
package org.hawkular.btm.tests.client.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.Producer;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author gbrown
 */
public class ClientCamelJMSTest extends ClientCamelTestBase {

    private ProducerTemplate template;

    @Override
    protected void initContext(CamelContext context) throws Exception {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        // Note we can explicit name the component
        context.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        template = context.createProducerTemplate();

        super.initContext(context);
    }

    @Override
    protected RouteBuilder getRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:inboundq").to("jms:topic:outboundt")
                        .to("language:simple:" + URLEncoder.encode("Hello", "UTF-8"));

                from("jms:topic:outboundt").log("LOG: ${body}");
            }
        };
    }

    @Test
    public void testJMSRequestOnly() {
        try {
            template.sendBody("jms:queue:inboundq", "Test Message");
        } catch (Exception e) {
            fail("Failed to send test message: " + e);
        }

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions - one btxn represents the test sender
        assertEquals(3, getTestBTMServer().getBusinessTransactions().size());

        Consumer queueConsumer = null;
        Consumer topicConsumer = null;
        Component testComponent = null;

        for (BusinessTransaction btxn : getTestBTMServer().getBusinessTransactions()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(btxn));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (!btxn.getNodes().isEmpty()) {
                if (btxn.getNodes().get(0).getClass() == Consumer.class) {
                    Consumer consumer = (Consumer) btxn.getNodes().get(0);

                    if (consumer.getUri().equals("queue://inboundq")) {
                        queueConsumer = consumer;
                    } else if (consumer.getUri().equals("topic://outboundt")) {
                        topicConsumer = consumer;
                    }
                } else if (btxn.getNodes().get(0).getClass() == Component.class) {
                    testComponent = (Component) btxn.getNodes().get(0);
                }
            }
        }

        assertNotNull("queueConsumer null", queueConsumer);
        assertNotNull("topicConsumer null", topicConsumer);
        assertNotNull("testComponent null", testComponent);

        Producer topicProducer = null;
        Producer queueProducer = null;

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(queueConsumer.getNodes(), Producer.class, producers);
        findNodes(testComponent.getNodes(), Producer.class, producers);

        assertEquals("Expecting 2 producers", 2, producers.size());

        for (Producer p : producers) {
            if (p.getUri().equals("queue://inboundq")) {
                queueProducer = p;
            } else if (p.getUri().equals("topic://outboundt")) {
                topicProducer = p;
            }
        }

        assertNotNull("queueProducer null", queueProducer);
        assertNotNull("topicProducer null", topicProducer);

        // Check details
        String publish = queueProducer.getDetails().get("btm_publish");
        if (publish != null) {
            assertEquals("false", publish);
        }

        assertEquals("true", topicProducer.getDetails().get("btm_publish"));

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(topicProducer, topicConsumer);
        checkInteractionCorrelationIdentifiers(queueProducer, queueConsumer);

        // Check headers
        assertFalse("queueProducer has no headers", queueProducer.getRequest().getHeaders().isEmpty());
        assertFalse("topicProducer has no headers", topicProducer.getRequest().getHeaders().isEmpty());
        assertFalse("topicConsumer has no headers", topicConsumer.getRequest().getHeaders().isEmpty());
        assertFalse("queueConsumer has no headers", queueConsumer.getRequest().getHeaders().isEmpty());
    }

    @Test
    public void testJMSRequestResponse() {
        Object resp = null;
        try {
            resp = template.sendBody("jms:queue:inboundq", ExchangePattern.InOut, "Test Message");
        } catch (Exception e) {
            fail("Failed to send test message: " + e);
        }

        assertEquals("Hello", resp);

        try {
            synchronized (this) {
                wait(2000);
            }
        } catch (Exception e) {
            fail("Failed to wait for btxns to store");
        }

        // Check stored business transactions - one btxn represents the test sender
        assertEquals(3, getTestBTMServer().getBusinessTransactions().size());

        Consumer queueConsumer = null;
        Consumer topicConsumer = null;
        Component testComponent = null;

        for (BusinessTransaction btxn : getTestBTMServer().getBusinessTransactions()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            try {
                System.out.println("BTXN=" + mapper.writeValueAsString(btxn));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            if (!btxn.getNodes().isEmpty()) {
                if (btxn.getNodes().get(0).getClass() == Consumer.class) {
                    Consumer consumer = (Consumer) btxn.getNodes().get(0);

                    if (consumer.getUri().equals("queue://inboundq")) {
                        queueConsumer = consumer;
                    } else if (consumer.getUri().equals("topic://outboundt")) {
                        topicConsumer = consumer;
                    }
                } else if (btxn.getNodes().get(0).getClass() == Component.class) {
                    testComponent = (Component) btxn.getNodes().get(0);
                }
            }
        }

        assertNotNull("queueConsumer null", queueConsumer);
        assertNotNull("topicConsumer null", topicConsumer);
        assertNotNull("testComponent null", testComponent);

        Producer topicProducer = null;
        Producer queueProducer = null;

        List<Producer> producers = new ArrayList<Producer>();
        findNodes(queueConsumer.getNodes(), Producer.class, producers);
        findNodes(testComponent.getNodes(), Producer.class, producers);

        assertEquals("Expecting 2 producers", 2, producers.size());

        for (Producer p : producers) {
            if (p.getUri().equals("queue://inboundq")) {
                queueProducer = p;
            } else if (p.getUri().equals("topic://outboundt")) {
                topicProducer = p;
            }
        }

        assertNotNull("queueProducer null", queueProducer);
        assertNotNull("topicProducer null", topicProducer);

        // Check details
        String publish = queueProducer.getDetails().get("btm_publish");
        if (publish != null) {
            assertEquals("false", publish);
        }

        assertEquals("true", topicProducer.getDetails().get("btm_publish"));

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(topicProducer, topicConsumer);
        checkInteractionCorrelationIdentifiers(queueProducer, queueConsumer);

        // Check headers
        assertFalse("queueProducer has no headers", queueProducer.getRequest().getHeaders().isEmpty());
        assertFalse("topicProducer has no headers", topicProducer.getRequest().getHeaders().isEmpty());
        assertFalse("topicConsumer has no headers", topicConsumer.getRequest().getHeaders().isEmpty());
        assertFalse("queueConsumer has no headers", queueConsumer.getRequest().getHeaders().isEmpty());
    }
}
