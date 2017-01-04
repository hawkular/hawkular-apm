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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author gbrown
 */
public class ElasticsearchClientTest {

    private static final String TESTHOSTS = "testhosts";

    @Test
    public void testDefaultElasticsearchHostNull() {
        System.clearProperty(ElasticsearchClient.ELASTICSEARCH_HOSTS);
        ElasticsearchClient client = new ElasticsearchClient();
        assertEquals(ElasticsearchClient.ELASTICSEARCH_HOSTS_DEFAULT, client.getHosts());
    }

    @Test
    public void testDefaultElasticsearchHostEmptyString() {
        System.setProperty(ElasticsearchClient.ELASTICSEARCH_HOSTS, "");
        ElasticsearchClient client = new ElasticsearchClient();
        assertEquals(ElasticsearchClient.ELASTICSEARCH_HOSTS_DEFAULT, client.getHosts());
    }

    @Test
    public void testDefaultElasticsearchHostValue() {
        System.setProperty(ElasticsearchClient.ELASTICSEARCH_HOSTS, TESTHOSTS);
        ElasticsearchClient client = new ElasticsearchClient();
        assertEquals(TESTHOSTS, client.getHosts());
    }

}
