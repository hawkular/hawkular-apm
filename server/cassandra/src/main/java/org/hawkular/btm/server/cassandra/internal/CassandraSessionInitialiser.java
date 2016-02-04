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
package org.hawkular.btm.server.cassandra.internal;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.btm.server.cassandra.CassandraClient;

/**
 * @author gbrown
 */
@Startup
@Singleton
@ApplicationScoped
public class CassandraSessionInitialiser {

    private static final Logger log = Logger.getLogger(CassandraSessionInitialiser.class.getName());

    @Inject
    private CassandraClient client;

    @PostConstruct
    public void init() {
        log.fine("Initialised cassandra client=" + client);
    }
}
