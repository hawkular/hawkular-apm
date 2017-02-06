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
package org.hawkular.apm.client.api.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.ServiceStatus;
import org.hawkular.apm.api.utils.PropertyUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the abstract based class for REST client implementations.
 *
 * @author gbrown
 */
public class AbstractRESTClient implements ServiceStatus {
    private static final Logger log = Logger.getLogger(AbstractRESTClient.class.getName());

    private static final String HAWKULAR_TENANT = "Hawkular-Tenant";
    protected static final ObjectMapper mapper = new ObjectMapper();
    private static final Base64.Encoder encoder = Base64.getEncoder();

    private String authorization;
    private String uri;

    /**
     * By default rest client tries to find username and password in environmental variables.
     * @param uriProperty
     */
    public AbstractRESTClient(String uriProperty) {
        this(PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_USERNAME),
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_PASSWORD),
                PropertyUtil.getProperty(uriProperty, PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_URI)));
    }

    public AbstractRESTClient(String username, String password, String url) {
        if (url != null && !url.isEmpty() && url.charAt(url.length() - 1) != '/') {
            url += '/';
        }

        this.authorization = basicAuthorization(username, password);
        this.uri = url;
    }

    @Override
    public boolean isAvailable() {
        // Check URI is specified and starts with http, so either http: or https:
        return uri != null && uri.startsWith("http");
    }

    public void setAuthorization(String username, String password) {
        this.authorization = basicAuthorization(username, password);
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

        if (authorization != null) {
            connection.setRequestProperty("Authorization", authorization);
        }
    }

    public URL getUrl(String path, Object... args) {
        return getUrl(String.format(path, args));
    }

    public URL getUrl(String path) {
        try {
            return new URL(uri + "hawkular/apm/" + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T withContext(String tenantId, URL url, Function<HttpURLConnection, T> function) {
        HttpURLConnection connection = null;
        try {
            connection = getConnectionForGetRequest(tenantId, url);
            return function.apply(connection);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public <T> T getResultsForUrl(String tenantId, TypeReference<T> typeReference, String path, Object... parameters) {
        return withContext(tenantId, getUrl(path, parameters), (connection) -> parseResultsIntoJson(connection, typeReference));
    }

    public <T> T parseResultsIntoJson(HttpURLConnection connection, TypeReference<T> typeReference) {
        try {
            String response = getResponse(connection);
            if (connection.getResponseCode() == 200) {
                if (log.isLoggable(Logger.Level.FINEST)) {
                    log.finest("Returned json=[" + response + "]");
                }
                if (!response.trim().isEmpty()) {
                    try {
                        return mapper.readValue(response, typeReference);
                    } catch (Throwable t) {
                        log.log(Logger.Level.SEVERE, "Failed to deserialize", t);
                    }
                }
            } else {
                if (log.isLoggable(Logger.Level.FINEST)) {
                    log.finest("Failed to get results: status=["
                            + connection.getResponseCode() + "]:"
                            + connection.getResponseMessage());
                }
            }
        } catch (Exception e) {
            log.log(Logger.Level.SEVERE, "Failed to get results", e);
        }
        return null;
    }

    public <T> T getResultsForUrl(String tenantId, TypeReference<T> typeReference, String path, Criteria criteria) {
        return getResultsForUrl(tenantId, typeReference, path, encodedCriteria(criteria));
    }
    public <T> T getResultsForUrl(String tenantId, TypeReference<T> typeReference, String path, Criteria criteria, Object arg) {
        return getResultsForUrl(tenantId, typeReference, path, encodedCriteria(criteria), arg);
    }

    public String encodedCriteria(Criteria criteria) {
        try {
            return URLEncoder.encode(mapper.writeValueAsString(criteria), "UTF-8");
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpURLConnection getConnectionForGetRequest(String tenantId, URL url) throws IOException {
        return getConnectionForRequest(tenantId, url, "GET");
    }

    public HttpURLConnection getConnectionForRequest(String tenantId, URL url, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setAllowUserInteraction(false);
        addHeaders(connection, tenantId);
        return connection;
    }

    public String getResponse(HttpURLConnection connection) throws IOException {
        InputStream is = connection.getInputStream();
        String response;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            response = buffer.lines().collect(Collectors.joining("\n"));
        }
        return response;
    }

    public int postAsJsonTo(String tenantId, String path, Object toSerialize) {
        URL url = getUrl(path);
        return withJsonPayloadAndResults("POST", tenantId, url, toSerialize, (connection) -> {
            try {
                return connection.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
                log.log(Logger.Level.SEVERE, String.format("Failed to post to [%s]", url), e);
            }
            return 0;
        });
    }

    public <T> T withJsonPayloadAndResults(String method, String tenantId, URL url, Object toSerialize, Function<HttpURLConnection, T> function) {
        return withContext(tenantId, url, (connection) -> {
            try {
                connection.setRequestMethod(method);

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setRequestProperty("Content-Type", "application/json");

                OutputStream os = connection.getOutputStream();
                os.write(mapper.writeValueAsBytes(toSerialize));
                os.flush();
                os.close();

                return function.apply(connection);
            } catch (IOException e) {
                e.printStackTrace();
                log.log(Logger.Level.SEVERE, String.format("Failed to post to [%s]", url), e);
            }
            return null;
        });
    }

    public void clear(String tenantId, String path) {
        if (log.isLoggable(Logger.Level.FINEST)) {
            log.finest(String.format("Clear service at path [%s] for tenant [%s]", path, tenantId));
        }

        URL url = getUrl(path);
        withContext(tenantId, url, (connection) -> {
            try {
                connection.setRequestMethod("DELETE");
                if (connection.getResponseCode() == 200) {
                    if (log.isLoggable(Logger.Level.FINEST)) {
                        log.finest(String.format("Service at [%s] cleared", path));
                    }
                } else {
                    if (log.isLoggable(Logger.Level.FINEST)) {
                        log.warning("Failed to clear analytics: status=["
                                + connection.getResponseCode() + "]:"
                                + connection.getResponseMessage());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                log.log(Logger.Level.SEVERE, String.format("Failed to send 'clear' request to service [%s]", path), e);
            }
            return null;
        });
    }

    private String basicAuthorization(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        return "Basic " + encoder.encodeToString((username + ":" + password).getBytes());
    }
}
