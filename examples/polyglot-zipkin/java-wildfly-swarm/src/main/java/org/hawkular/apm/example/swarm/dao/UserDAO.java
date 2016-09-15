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

package org.hawkular.apm.example.swarm.dao;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.SpanId;

/**
 * @author Pavol Loffay
 */
@Singleton
public class UserDAO {

    private static final String KEYSPACE = "wildfly_swarm";
    private static final String TABLE = "users";

    private final Brave brave;
    private final Session session;


    @Inject
    public UserDAO(Brave brave) {
        this.brave = brave;
        Cluster cluster = Cluster.builder().addContactPoint("cassandra").build();
        this.session = cluster.connect();
        init();
    }

    private void init() {
        PreparedStatement preparedStatement = session.prepare("DROP KEYSPACE IF EXISTS " + KEYSPACE).enableTracing();
        executeWithClientSpan(preparedStatement.bind());

        preparedStatement = session.prepare("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE + " WITH REPLICATION = " +
                "{'class' : 'SimpleStrategy', 'replication_factor' : 1}").enableTracing();
        executeWithClientSpan(preparedStatement.bind());

        preparedStatement = session.prepare("USE " + KEYSPACE).enableTracing();
        executeWithClientSpan(preparedStatement.bind());

        preparedStatement = session.prepare("CREATE TABLE " + KEYSPACE + "." + TABLE +
                "(id text PRIMARY KEY, name text)").enableTracing();
        executeWithClientSpan(preparedStatement.bind());
    }

    public User createUser(User user) {
        user.setId(UUID.randomUUID().toString());

        Statement statement = QueryBuilder.insertInto(KEYSPACE, TABLE)
                .value("id", user.getId())
                .value("name", user.getName())
                .enableTracing();

        executeWithClientSpan(statement);

        return user;
    }

    public User getUser(String id) {
        Statement statement = QueryBuilder.select()
                .from(KEYSPACE, TABLE)
                .where(QueryBuilder.eq("id", id))
                .enableTracing();

        ResultSet resultSet = executeWithClientSpan(statement);

        User user = null;
        for (Row row: resultSet) {
            user = new User(row.getString("id"), row.getString("name"));
        }

        return user;
    }

    public Collection<User> getAllUsers() {
        Statement statement = QueryBuilder.select()
                .from(KEYSPACE, TABLE)
                .enableTracing();

        ResultSet resultSet = executeWithClientSpan(statement);

        List<User> users = new ArrayList<>();

        for (Row row: resultSet) {
            users.add(new User(row.getString("id"), row.getString("name")));
        }

        return users;
    }

    private ResultSet executeWithClientSpan(Statement statement) {

        ClientTracer clientTracer = brave.clientTracer();
        SpanId spanId = clientTracer.startNewSpan(statement.toString());

        ByteBuffer traceHeaders = ByteBuffer.wrap(spanId.bytes());
        statement.setOutgoingPayload(Collections.singletonMap("zipkin", traceHeaders));

//        clientTracer.set(serviceName);
        clientTracer.setClientSent();
        ResultSet resultSet = session.execute(statement);
        clientTracer.setClientReceived();

        return resultSet;
    }
}
