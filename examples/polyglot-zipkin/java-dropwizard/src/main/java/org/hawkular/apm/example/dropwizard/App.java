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

package org.hawkular.apm.example.dropwizard;

import javax.ws.rs.client.Client;

import org.hawkular.apm.example.dropwizard.dao.UserDAO;
import org.hawkular.apm.example.dropwizard.rest.AsyncHandler;
import org.hawkular.apm.example.dropwizard.rest.HelloHandler;
import org.hawkular.apm.example.dropwizard.rest.SyncHandler;
import org.hawkular.apm.example.dropwizard.rest.UsersHandler;
import org.hawkular.apm.example.dropwizard.util.DatabaseUtils;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.mysql.MySQLStatementInterceptorManagementBean;
import com.smoketurner.dropwizard.zipkin.ZipkinBundle;
import com.smoketurner.dropwizard.zipkin.ZipkinFactory;
import com.smoketurner.dropwizard.zipkin.client.ZipkinClientBuilder;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

/**
 * @author Pavol Loffay
 */
public class App extends Application<AppConfiguration> {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public String getName() {
        return "dropwizard";
    }

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor()));

        bootstrap.addBundle(new ZipkinBundle<AppConfiguration>(getName()) {
            @Override
            public ZipkinFactory getZipkinFactory(AppConfiguration configuration) {
                return configuration.getZipkinFactory();
            }
        });
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) throws Exception {

        configuration.getZipkinClient().setTimeout(Duration.seconds(50));
        configuration.getZipkinClient().setConnectionRequestTimeout(Duration.seconds(50));

        final Brave brave = configuration.getZipkinFactory().build(environment);

        final Client client = new ZipkinClientBuilder(environment, brave)
                .build(configuration.getZipkinClient());

        new MySQLStatementInterceptorManagementBean(brave.clientTracer());

        /**
         * Database
         */
        createDatabase();
        DatabaseUtils.executeDatabaseScript("init.sql");

        UserDAO userDAO = new UserDAO();

        // Register resources
        environment.jersey().register(new HelloHandler());
        environment.jersey().register(new SyncHandler(client));
        environment.jersey().register(new AsyncHandler(client, brave));
        environment.jersey().register(new UsersHandler(userDAO, client, brave));
    }

    private DB createDatabase() throws ManagedProcessException {

        DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
        configBuilder.setPort(3306);
        configBuilder.setDataDir("target/mariaDB");
        configBuilder.setBaseDir("target/mariaDB");
        DB database = DB.newEmbeddedDB(configBuilder.build());
        database.start();

        return database;
    }
}
