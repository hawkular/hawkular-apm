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
package org.hawkular.btm.server.jms;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.hawkular.btm.api.services.Publisher;
import org.hawkular.btm.server.jms.log.MsgLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This abstract class represents a JMS publisher.
 *
 * @author gbrown
 */
public abstract class AbstractPublisherJMS<T> implements Publisher<T> {

    private static final Logger log = Logger.getLogger(AbstractPublisherJMS.class.getName());

    private static final int DEFAULT_INITIAL_RETRY_COUNT = 3;

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    private static ObjectMapper mapper = new ObjectMapper();

    private Connection connection;
    private Session session;
    private MessageProducer producer;

    private int initialRetryCount = DEFAULT_INITIAL_RETRY_COUNT;

    /**
     * This method returns the destination associated with the publisher.
     *
     * @return The destination
     */
    protected abstract String getDestinationURI();

    /**
     * @return the initialRetryCount
     */
    @Override
    public int getInitialRetryCount() {
        return initialRetryCount;
    }

    /**
     * @param initialRetryCount the initialRetryCount to set
     */
    public void setInitialRetryCount(int initialRetryCount) {
        this.initialRetryCount = initialRetryCount;
    }

    @PostConstruct
    public void init() {
        try {
            InitialContext context = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("java:/JmsXA");
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);    // TODO: Transacted?
            Destination destination = (Destination) context.lookup(getDestinationURI());
            producer = session.createProducer(destination);
            connection.start();
        } catch (Exception e) {
            msgLog.errorFailedToInitPublisher(getDestinationURI(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.Publisher#publish(java.lang.String, java.util.List, long)
     */
    @Override
    public void publish(String tenantId, List<T> items, int retryCount, long delay) throws Exception {
        String data = mapper.writeValueAsString(items);

        TextMessage tm = session.createTextMessage(data);

        if (tenantId != null) {
            tm.setStringProperty("tenant", tenantId);
        }

        tm.setIntProperty("retryCount", retryCount);

        if (delay > 0) {
            tm.setLongProperty("_AMQ_SCHED_DELIVERY", System.currentTimeMillis() + delay);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Publish: " + tm);
        }

        producer.send(tm);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.Publisher#publish(java.lang.String, java.util.List)
     */
    @Override
    public void publish(String tenantId, List<T> items) throws Exception {
        publish(tenantId, items, getInitialRetryCount(), 0);
    }

    @PreDestroy
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            msgLog.errorFailedToClosePublisher(getDestinationURI(), e);
        }
    }

}
