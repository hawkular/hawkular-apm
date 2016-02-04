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
package org.hawkular.btm.server.cassandra;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Session;

/**
 * @author gbrown
 */
@Singleton
public class CassandraClient {

    private static final Logger log = Logger.getLogger(CassandraClient.class.getName());

    private Session session;

    @PostConstruct
    public void init() {
        // TODO: Initialise session when client first created - however this results
        // in a null session, so need retry until session found, and blocking for
        // client's trying to retrieve the session - but also timeout if fails to
        // get session?
    }

    public synchronized Session getSession() {
        if (session == null) {
            try {
                // TODO: Get configuration - should really be a common module for
                // accessing cassandra session with single approach to config
                session = new Cluster.Builder()
                    .addContactPoints("127.0.0.1")
                    .withPort(new Integer("9042"))
                    .withProtocolVersion(ProtocolVersion.V3)
                    .withoutJMXReporting()
                    .build().connect();

                // Configure schema
                configureSchema();

            } catch (Exception e) {
                e.printStackTrace();
            }
            log.fine("Initialised cassandra session=" + session);
        }

        return session;
    }

    /**
     * Configure the schema for BTM.
     */
    protected void configureSchema() {
        try {
            InputStream input = getClass().getResourceAsStream("/hawkular_btm.cql");
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
                String content = buffer.lines().collect(Collectors.joining("\n"));

                // we split the statements by "--#", as it's done in other Hawkular projects.
                for (String cql : content.split("(?m)^-- #.*$")) {
                    if (!cql.startsWith("--")) { // if it doesn't look like a comment, execute it
                        session.execute(cql);
                        if (log.isLoggable(Level.FINE)) {
                            log.fine("Executed CQL = "+cql);
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Could not get the initialized session.");
        }
    }

    @PreDestroy
    public void close() {
        if (session != null) {
            session.close();
        }
    }
}
