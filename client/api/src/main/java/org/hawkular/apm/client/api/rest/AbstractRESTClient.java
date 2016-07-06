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
package org.hawkular.apm.client.api.rest;

import java.net.HttpURLConnection;
import java.util.Base64;

import org.hawkular.apm.api.services.ServiceStatus;
import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * This class provides the abstract based class for REST client implementations.
 *
 * @author gbrown
 */
public class AbstractRESTClient implements ServiceStatus {

    private static final String HAWKULAR_TENANT = "Hawkular-Tenant";

    private String username = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_USERNAME);
    private String password = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_PASSWORD);

    private String authorization = null;

    private String uri;

    private static final Base64.Encoder encoder = Base64.getEncoder();

    public AbstractRESTClient(String uriProperty) {
        uri = PropertyUtil.getProperty(uriProperty,
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI));

        if (uri != null && !uri.isEmpty() && uri.charAt(uri.length() - 1) != '/') {
            uri = uri + '/';
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.api.services.ServiceStatus#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        // Check URI is specified and starts with http, so either http: or https:
        return uri != null && uri.startsWith("http");
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;

        // Clear any previously computed authorization string
        this.authorization = null;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;

        // Clear any previously computed authorization string
        this.authorization = null;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Add the header values to the supplied connection.
     *
     * @param connection The connection
     * @param tenantId The optional tenant id
     */
    protected void addHeaders(HttpURLConnection connection, String tenantId) {
        if (tenantId == null) {
            // Check if default tenant provided as property
            tenantId = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_TENANT);
        }

        if (tenantId != null) {
            connection.setRequestProperty(HAWKULAR_TENANT, tenantId);
        }

        if (authorization == null && username != null) {
            String authString = username + ":" + password;
            String encoded = encoder.encodeToString(authString.getBytes());

            authorization = "Basic " + encoded;
        }

        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
    }

}
