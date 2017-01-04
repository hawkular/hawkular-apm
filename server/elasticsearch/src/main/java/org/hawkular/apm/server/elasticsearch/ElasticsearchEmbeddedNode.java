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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

/**
 * This class represents an Elasticsearch embedded node.
 *
 * User: imk@redhat.com
 * Date: 21/06/14
 * Time: 23:41
 */
public final class ElasticsearchEmbeddedNode {

    private static final String HAWKULAR_ELASTICSEARCH_PROPERTIES = "hawkular-elasticsearch.properties";

    private static final Logger log = Logger.getLogger(ElasticsearchEmbeddedNode.class.getName());

    private Client client;
    private Node node;

    /**
     * The default constructor.
     */
    public ElasticsearchEmbeddedNode() {
    }

    /**
     * This method initializes the node.
     */
    protected void initNode() {
        /**
         * quick fix for integration tests. if hosts property set to "embedded" then a local node is start.
         * maven dependencies need to be defined correctly for this to work
         */
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        try {
            // Need to use the classloader for Elasticsearch to pick up the property files when
            // running in an OSGi environment
            Thread.currentThread().setContextClassLoader(TransportClient.class.getClassLoader());

            final Properties properties = new Properties();
            try {
                InputStream stream = null;

                if (System.getProperties().containsKey("jboss.server.config.dir")) {
                    File file = new File(System.getProperty("jboss.server.config.dir") + File.separatorChar +
                            HAWKULAR_ELASTICSEARCH_PROPERTIES);
                    stream = new FileInputStream(file);
                } else {
                    stream = this.getClass().getResourceAsStream(File.separatorChar +
                                    HAWKULAR_ELASTICSEARCH_PROPERTIES);
                }
                properties.load(stream);
                stream.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to load elasticsearch properties", e);
            }

            node = NodeBuilder.nodeBuilder()
                    .settings(ImmutableSettings.settingsBuilder()
                            .put(properties)).node();

            node.start();
            client = node.client();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Initialized Elasticsearch node=" + node + " client=" + client);
        }
    }

    /**
     * This method closes the elasticsearch node.
     */
    public void close() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Close Elasticsearch node=" + node + " client=" + client);
        }

        if (client != null) {
            client.close();
            client = null;
        }

        if (node != null) {
            node.stop();
            node = null;
        }
    }

    /**
     * This method returns the Elasticsearch client associated with the
     * embedded node.
     *
     * @return The client
     */
    public synchronized Client getClient() {
        if (client == null) {
            initNode();
        }
        return client;
    }
}
