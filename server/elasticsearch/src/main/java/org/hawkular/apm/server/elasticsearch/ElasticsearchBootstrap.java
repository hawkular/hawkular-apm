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
package org.hawkular.apm.server.elasticsearch;

import static javax.ejb.ConcurrencyManagementType.BEAN;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author gbrown
 */
@ApplicationScoped
@Singleton
@Startup
@ConcurrencyManagement(BEAN)
public class ElasticsearchBootstrap {

    private static final Logger log = Logger.getLogger(ElasticsearchBootstrap.class.getName());

    @Inject
    private ElasticsearchClient client;

    @PostConstruct
    public void init() {
        try {
            // Initialise default tenant
            client.initTenant(null);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to initialise Elasticsearch", e);
        }
    }
}
