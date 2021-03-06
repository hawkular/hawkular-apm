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
package org.hawkular.apm.tests.agent.opentracing.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.hawkular.apm.tests.agent.opentracing.common.OpenTracingAgentTestBase;
import org.hawkular.apm.tests.common.Wait;
import org.junit.BeforeClass;
import org.junit.Test;

import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;

/**
 * @author gbrown
 */
public class JavaxJMSITest extends OpenTracingAgentTestBase {

    private static ConnectionFactory connectionFactory;
    private static ExecutorService executorService;

    @BeforeClass
    public static void initClass() throws Exception {
        executorService = Executors.newCachedThreadPool();

        connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
    }

    @Test
    public void testQueueRequestOnly() throws JMSException, InterruptedException, ExecutionException {
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Future<String> serverResult = executorService.submit(new JMSQueueAsyncServer());

        MessageProducer producer = session.createProducer(session.createQueue(JMSAsyncServer.TEST_QUEUE));

        TextMessage request = session.createTextMessage(JMSAsyncServer.TEST_MESSAGE);

        producer.send(request);

        producer.close();
        session.close();
        connection.close();

        assertEquals(JMSAsyncServer.TEST_MESSAGE, serverResult.get());

        Wait.until(() -> getTracer().finishedSpans().size() == 2);

        List<MockSpan> spans = getTracer().finishedSpans();
        assertEquals(2, spans.size());

        assertEquals(Tags.SPAN_KIND_CLIENT, spans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
        assertEquals(Tags.SPAN_KIND_SERVER, spans.get(1).tags().get(Tags.SPAN_KIND.getKey()));
        assertEquals("queue://TestQueue", spans.get(0).tags().get("mom.url"));
        assertEquals("queue://TestQueue", spans.get(1).tags().get("mom.url"));
    }

    @Test
    public void testTopicPublish() throws JMSException, InterruptedException, ExecutionException {
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        CountDownLatch latch = new CountDownLatch(2);

        Future<String> server1Result = executorService.submit(new JMSTopicAsyncServer(latch));
        Future<String> server2Result = executorService.submit(new JMSTopicAsyncServer(latch));

        MessageProducer producer = session.createProducer(session.createTopic(JMSAsyncServer.TEST_TOPIC));

        TextMessage request = session.createTextMessage(JMSAsyncServer.TEST_MESSAGE);

        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));

        producer.send(request);

        producer.close();
        session.close();
        connection.close();

        assertEquals(JMSAsyncServer.TEST_MESSAGE, server1Result.get());
        assertEquals(JMSAsyncServer.TEST_MESSAGE, server2Result.get());

        Wait.until(() -> getTracer().finishedSpans().size() == 3);

        List<MockSpan> spans = getTracer().finishedSpans();
        assertEquals(3, spans.size());

        assertEquals(1, spans.stream().filter(s -> s.tags().get(Tags.SPAN_KIND.getKey()).equals(Tags.SPAN_KIND_CLIENT)).count());
        assertEquals(2, spans.stream().filter(s -> s.tags().get(Tags.SPAN_KIND.getKey()).equals(Tags.SPAN_KIND_SERVER)).count());
        assertEquals(3, spans.stream().filter(s -> s.tags().containsKey("mom.url")).count());
        assertEquals(1, spans.stream().map(s -> s.tags().get("mom.url")).distinct().count());
    }

    public class JMSQueueAsyncServer extends JMSAsyncServer {

        public JMSQueueAsyncServer() {
            super(null);
        }

        @Override
        protected Destination createDestination(Session session) throws JMSException {
            return session.createQueue(JMSAsyncServer.TEST_QUEUE);
        }
    }

    public class JMSTopicAsyncServer extends JMSAsyncServer {

        public JMSTopicAsyncServer(CountDownLatch latch) {
            super(latch);
        }

        @Override
        protected Destination createDestination(Session session) throws JMSException {
            return session.createTopic(JMSAsyncServer.TEST_TOPIC);
        }
    }

    public abstract class JMSAsyncServer implements Callable<String> {

        private static final String TEST_MESSAGE = "TestMessage";
        private static final String TEST_QUEUE = "TestQueue";
        private static final String TEST_TOPIC = "TestTopic";
        private static final String SUFFIX = "Reply";

        private CountDownLatch latch;

        public JMSAsyncServer(CountDownLatch latch) {
            this.latch = latch;
        }

        protected abstract Destination createDestination(Session session) throws JMSException;

        @Override
        public String call() throws JMSException, InterruptedException {
            String result = null;
            Connection connection = connectionFactory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageConsumer consumer = session.createConsumer(createDestination(session));

            SynchronousQueue<Message> sq = new SynchronousQueue<>();

            consumer.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message arg0) {
                    sq.offer(arg0);
                }
            });

            if (latch != null) {
                latch.countDown();
            }

            Message message = sq.poll(5000, TimeUnit.MILLISECONDS);

            if (message instanceof TextMessage) {
                TextMessage mesg = (TextMessage) message;

                if (mesg.getJMSReplyTo() != null) {
                    MessageProducer producer = session.createProducer(mesg.getJMSReplyTo());

                    TextMessage response = session.createTextMessage(mesg.getText() + SUFFIX);

                    producer.send(response);

                    producer.close();
                }

                result = mesg.getText();
            }

            consumer.close();
            session.close();
            connection.close();

            return result;
        }
    }

}
