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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.hawkular.apm.api.services.StoreException;
import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * This class represents the ElasticSearch client.
 */
public class ElasticsearchClient {

    private static final String HAWKULAR_APM_MAPPING_JSON = "hawkular-apm-mapping.json";

    /**
     * Settings for the index this store is related to.
     */
    public static final String SETTINGS = "settings";

    /**
     * Type mappings for the index this store is related to.
     */
    public static final String MAPPINGS = "mappings";

    /**
     * The default settings.
     */
    public static final String DEFAULT_SETTING = "_default_";

    private static final Logger log = Logger.getLogger(ElasticsearchClient.class.getName());

    private Client client;

    /**
     * Property name for Elasticsearch hosts configuration.
     */
    public static final String ELASTICSEARCH_HOSTS = "HAWKULAR_APM_ELASTICSEARCH_HOSTS";

    /**
     * Default Elasticsearch hosts configuration.
     */
    public static final String ELASTICSEARCH_HOSTS_DEFAULT = "embedded";

    private String hosts;

    /**
     * Property name for Elasticsearch cluster configuration.
     */
    public static final String ELASTICSEARCH_CLUSTER = "HAWKULAR_APM_ELASTICSEARCH_CLUSTER";

    /**
     * Default Elasticsearch cluster configuration.
     */
    public static final String ELASTICSEARCH_CLUSTER_DEFAULT = "elasticsearch";

    private String cluster;

    private static final Object SYNC = new Object();

    private static ElasticsearchEmbeddedNode node = null;

    private static Set<String> knownIndices = new HashSet<>();

    private static ElasticsearchClient singleton;

    /**
     * Default constructor.
     */
    protected ElasticsearchClient() {
        if (PropertyUtil.getProperty("HAWKULAR_APM_DATA_DIR") == null
                && System.getProperty("jboss.server.data.dir") != null) {
            System.setProperty("HAWKULAR_APM_DATA_DIR", System.getProperty("jboss.server.data.dir"));
        }

        hosts = PropertyUtil.getProperty(ELASTICSEARCH_HOSTS);

        if (hosts == null || hosts.trim().isEmpty()) {
            hosts = ELASTICSEARCH_HOSTS_DEFAULT;
        }

        cluster = PropertyUtil.getProperty(ELASTICSEARCH_CLUSTER, ELASTICSEARCH_CLUSTER_DEFAULT);
    }

    /**
     * This method explicit instantiates the singleton. For use outside a CDI environment.
     *
     * @return The singleton
     */
    public static synchronized ElasticsearchClient getSingleton() {
        if (singleton == null) {
            singleton = new ElasticsearchClient();
            try {
                singleton.init();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to initialise Elasticsearch client", e);
            }
        }
        return singleton;
    }

    /**
     * This method sets the hosts.
     *
     * @return The hosts
     */
    public String getHosts() {
        return hosts;
    }

    /**
     * This method returns the hosts.
     *
     * @param hosts The hosts
     */
    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    /**
     * Initialize the client.
     *
     * @throws Exception Failed to initialize the client
     */
    @PostConstruct
    public void init() throws Exception {

        if (hosts == null) {
            throw new IllegalArgumentException("Hosts property not set ");
        }

        determineHostsAsProperty();

        /**
         * quick fix for integration tests. if hosts property set to "embedded" then a local node is start.
         * maven dependencies need to be defined correctly for this to work
         */

        if (hosts.startsWith("embedded")) {
            synchronized (SYNC) {
                if (node == null) {
                    node = new ElasticsearchEmbeddedNode();
                }
            }
            client = node.getClient();
        } else {
            String[] hostsArray = hosts.split(",");
            Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", cluster).build();
            client = new TransportClient(settings);

            for (String aHostsArray : hostsArray) {
                String s = aHostsArray.trim();
                String[] host = s.split(":");

                if (log.isLoggable(Level.FINE)) {
                    log.fine(" Connecting to elasticsearch host. [" + host[0] + ":" + host[1] + "]");
                }

                client = ((TransportClient)client).addTransportAddress(new InetSocketTransportAddress(host[0],
                        new Integer(host[1])));
            }
        }
    }

    public String getIndex(String tenantId) {
        if (tenantId == null) {
            return "apm-hawkular";
        }
        return "apm-" + tenantId.toLowerCase();
    }

    @SuppressWarnings("unchecked")
    public void initTenant(String tenantId) throws StoreException {
        String index = getIndex(tenantId);

        if (!knownIndices.contains(index)) {
            synchronized (knownIndices) {
                if (!knownIndices.contains(index)) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Initialise mappings for tenantId = " + tenantId);
                    }

                    InputStream s = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(HAWKULAR_APM_MAPPING_JSON);
                    if (s == null) {
                        s = ElasticsearchClient.class.getResourceAsStream("/" + HAWKULAR_APM_MAPPING_JSON);
                    }

                    if (s != null) {
                        try {
                            String jsonDefaultUserIndex = IOUtils.toString(s);
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("Mapping [" + jsonDefaultUserIndex + "]");
                            }

                            Map<String, Object> dataMap = XContentFactory.xContent(jsonDefaultUserIndex)
                                    .createParser(jsonDefaultUserIndex).mapAndClose();

                            if (createIndex(index, (Map<String, Object>) dataMap.get(SETTINGS))) {
                                if (log.isLoggable(Level.FINEST)) {
                                    log.finest("Index '" + index + "' created");
                                }
                                // refresh index
                                RefreshRequestBuilder refreshRequestBuilder = getClient().admin().indices()
                                        .prepareRefresh(index);
                                getClient().admin().indices().refresh(refreshRequestBuilder.request())
                                .actionGet();
                            } else if (log.isLoggable(Level.FINEST)) {
                                log.finest("Index '" + index + "' already exists. Doing nothing.");
                            }

                            // Apply mapping in case changes have occurred - however will only be done
                            // once per server session, for a particular index (i.e. tenant)
                            prepareMapping(index, (Map<String, Object>) dataMap.get(MAPPINGS));

                            knownIndices.add(index);
                        } catch (IOException ioe) {
                            throw new StoreException(ioe);
                        }
                    } else {
                        log.warning("Could not locate '" + HAWKULAR_APM_MAPPING_JSON
                                + "' index mapping file. Mapping file required to use elasticsearch");
                    }
                }
            }
        }
    }

    /**
     * This method applies the supplied mapping to the index.
     *
     * @param index The name of the index
     * @param defaultMappings The default mappings
     * @return true if the mapping was successful
     */
    @SuppressWarnings("unchecked")
    private boolean prepareMapping(String index, Map<String, Object> defaultMappings) {
        boolean success = true;

        for (Map.Entry<String, Object> stringObjectEntry : defaultMappings.entrySet()) {
            Map<String, Object> mapping = (Map<String, Object>) stringObjectEntry.getValue();
            if (mapping == null) {
                throw new RuntimeException("type mapping not defined");
            }
            PutMappingRequestBuilder putMappingRequestBuilder = client.admin().indices().preparePutMapping()
                    .setIndices(index);
            putMappingRequestBuilder.setType(stringObjectEntry.getKey());
            putMappingRequestBuilder.setSource(mapping);

            if (log.isLoggable(Level.FINE)) {
                log.fine("Elasticsearch create mapping for index '"
                        + index + " and type '" + stringObjectEntry.getKey() + "': " + mapping);
            }

            PutMappingResponse resp = putMappingRequestBuilder.execute().actionGet();

            if (resp.isAcknowledged()) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Elasticsearch mapping for index '"
                            + index + " and type '" + stringObjectEntry.getKey() + "' was acknowledged");
                }
            } else {
                success = false;
                log.warning("Elasticsearch mapping creation was not acknowledged for index '"
                        + index + " and type '" + stringObjectEntry.getKey() + "'");
            }
        }

        return success;
    }

    /**
     * Check if index is created. if not it will created it
     *
     * @param index The index
     * @return returns true if it just created the index. False if the index already existed
     */
    private boolean createIndex(String index, Map<String, Object> defaultSettings) {
        IndicesExistsResponse res = client.admin().indices().prepareExists(index).execute().actionGet();
        boolean created = false;
        if (!res.isExists()) {
            CreateIndexRequestBuilder req = client.admin().indices().prepareCreate(index);
            req.setSettings(defaultSettings);
            created = req.execute().actionGet().isAcknowledged();
            if (!created) {
                throw new RuntimeException("Could not create index [" + index + "]");
            }
        }

        return created;
    }

    /**
     * sets hosts if the _hosts propertey is determined to be a property placeholder
     * Throws IllegalArgumentException argument exception when nothing found
     */
    private void determineHostsAsProperty() {
        if (hosts.startsWith("${") && hosts.endsWith("}")) {
            String hostsProperty = hosts.substring(2, hosts.length() - 1);
            hosts = PropertyUtil.getProperty(hostsProperty);
            if (hosts == null) {
                throw new IllegalArgumentException("Could not find property '"
                        + hostsProperty + "'");
            }
        }

    }

    /**
     * The Elasticsearch client.
     *
     * @return The client
     */
    public Client getClient() {
        return client;
    }

    /**
     * Removes all data associated with tenant.
     *
     * @param tenantId index
     */
    public void clearTenant(String tenantId) {
        String index = getIndex(tenantId);

        synchronized (knownIndices) {
            IndicesAdminClient indices = client.admin().indices();

            boolean indexExists = indices.prepareExists(index)
                    .execute()
                    .actionGet()
                    .isExists();

            if (indexExists) {
                indices.prepareDelete(index)
                        .execute()
                        .actionGet();
            }

            knownIndices.remove(index);
        }
    }

    /**
     * This method closes the Elasticsearch client.
     *
     */
    @PreDestroy
    public void close() {
        if (node != null) {
            node.close();
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public String toString() {
        return "ElasticsearchClient[hosts='" + hosts + "']";
    }
}
