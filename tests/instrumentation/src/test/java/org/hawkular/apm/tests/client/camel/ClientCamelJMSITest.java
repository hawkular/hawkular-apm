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
package org.hawkular.apm.tests.client.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;
import org.hawkular.apm.tests.common.Wait;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ClientCamelJMSITest extends ClientCamelITestBase {

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
        template.sendBody("jms:queue:inboundq", "Test Message");

        Wait.until(() -> getApmMockServer().getTraces().size() == 3);

        // Check stored traces - one btxn represents the test sender
        assertEquals(3, getApmMockServer().getTraces().size());

        Consumer queueConsumer = null;
        Consumer topicConsumer = null;
        Component testComponent = null;

        for (Trace trace : getApmMockServer().getTraces()) {
            if (!trace.getNodes().isEmpty()) {
                if (trace.getNodes().get(0).getClass() == Consumer.class) {
                    Consumer consumer = (Consumer) trace.getNodes().get(0);

                    if (consumer.getUri().equals("queue://inboundq")) {
                        queueConsumer = consumer;
                    } else if (consumer.getUri().equals("topic://outboundt")) {
                        topicConsumer = consumer;
                    }
                } else if (trace.getNodes().get(0).getClass() == Component.class) {
                    testComponent = (Component) trace.getNodes().get(0);
                }
            }
        }

        assertNotNull("queueConsumer null", queueConsumer);
        assertNotNull("topicConsumer null", topicConsumer);
        assertNotNull("testComponent null", testComponent);

        Producer topicProducer = null;
        Producer queueProducer = null;

        List<Producer> producers = new ArrayList<Producer>();
        NodeUtil.findNodes(queueConsumer.getNodes(), Producer.class, producers);
        NodeUtil.findNodes(testComponent.getNodes(), Producer.class, producers);

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
        String publish = queueProducer.getProperties("apm_publish").iterator().next().getValue();
        if (publish != null) {
            assertEquals("false", publish);
        }

        assertEquals("true", topicProducer.getProperties("apm_publish").iterator().next().getValue());

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(topicProducer, topicConsumer);
        checkInteractionCorrelationIdentifiers(queueProducer, queueConsumer);

        // Check headers
        assertFalse("queueProducer has no headers", queueProducer.getIn().getHeaders().isEmpty());
        assertFalse("topicProducer has no headers", topicProducer.getIn().getHeaders().isEmpty());
        assertFalse("topicConsumer has no headers", topicConsumer.getIn().getHeaders().isEmpty());
        assertFalse("queueConsumer has no headers", queueConsumer.getIn().getHeaders().isEmpty());
    }

    @Test
    public void testJMSRequestResponse() {
        Object resp = template.sendBody("jms:queue:inboundq", ExchangePattern.InOut, "Test Message");

        assertEquals("Hello", resp);

        Wait.until(() -> getApmMockServer().getTraces().size() == 3);

        // Check stored traces - one btxn represents the test sender
        assertEquals(3, getApmMockServer().getTraces().size());

        Consumer queueConsumer = null;
        Consumer topicConsumer = null;
        Component testComponent = null;

        for (Trace trace : getApmMockServer().getTraces()) {
            if (!trace.getNodes().isEmpty()) {
                if (trace.getNodes().get(0).getClass() == Consumer.class) {
                    Consumer consumer = (Consumer) trace.getNodes().get(0);

                    if (consumer.getUri().equals("queue://inboundq")) {
                        queueConsumer = consumer;
                    } else if (consumer.getUri().equals("topic://outboundt")) {
                        topicConsumer = consumer;
                    }
                } else if (trace.getNodes().get(0).getClass() == Component.class) {
                    testComponent = (Component) trace.getNodes().get(0);
                }
            }
        }

        assertNotNull("queueConsumer null", queueConsumer);
        assertNotNull("topicConsumer null", topicConsumer);
        assertNotNull("testComponent null", testComponent);

        Producer topicProducer = null;
        Producer queueProducer = null;

        List<Producer> producers = new ArrayList<Producer>();
        NodeUtil.findNodes(queueConsumer.getNodes(), Producer.class, producers);
        NodeUtil.findNodes(testComponent.getNodes(), Producer.class, producers);

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
        String publish = queueProducer.getProperties("apm_publish").iterator().next().getValue();
        if (publish != null) {
            assertEquals("false", publish);
        }

        assertEquals("true", topicProducer.getProperties("apm_publish").iterator().next().getValue());

        // Check correlation identifiers match
        checkInteractionCorrelationIdentifiers(topicProducer, topicConsumer);
        checkInteractionCorrelationIdentifiers(queueProducer, queueConsumer);

        // Check headers
        assertFalse("queueProducer has no headers", queueProducer.getIn().getHeaders().isEmpty());
        assertFalse("topicProducer has no headers", topicProducer.getIn().getHeaders().isEmpty());
        assertFalse("topicConsumer has no headers", topicConsumer.getIn().getHeaders().isEmpty());
        assertFalse("queueConsumer has no headers", queueConsumer.getIn().getHeaders().isEmpty());

        // Check 'in' content
        assertTrue(queueProducer.getIn().getContent().containsKey("all"));
        assertEquals("Test Message", queueProducer.getIn().getContent().get("all").getValue());
        assertTrue(queueConsumer.getIn().getContent().containsKey("all"));
        assertEquals("Test Message", queueConsumer.getIn().getContent().get("all").getValue());
        assertTrue(topicProducer.getIn().getContent().containsKey("all"));
        assertEquals("Test Message", topicProducer.getIn().getContent().get("all").getValue());
        assertTrue(topicConsumer.getIn().getContent().containsKey("all"));
        assertEquals("Test Message", topicConsumer.getIn().getContent().get("all").getValue());

        // Check 'out' content
        assertNotNull(queueProducer.getOut());
        assertNotNull(queueProducer.getOut().getContent());
        assertTrue(queueProducer.getOut().getContent().containsKey("all"));
        assertEquals("Hello", queueProducer.getOut().getContent().get("all").getValue());
        assertNotNull(queueConsumer.getOut());
        assertNotNull(queueConsumer.getOut().getContent());
        assertTrue(queueConsumer.getOut().getContent().containsKey("all"));
        assertEquals("Hello", queueConsumer.getOut().getContent().get("all").getValue());
    }

    @Test
    public void testTraceIdPropagated() {
        template.sendBody("jms:queue:inboundq", ExchangePattern.InOut, "Test Message");

        Wait.until(() -> getApmMockServer().getTraces().size() == 3);

        // Check stored traces - one btxn represents the test sender
        assertEquals(3, getApmMockServer().getTraces().size());

        // Check only one trace id used for all trace fragments
        assertEquals(1, getApmMockServer().getTraces().stream().map(t -> {
            assertNotNull(t.getTraceId());
            return t.getTraceId();
        }).distinct().count());
    }
}
