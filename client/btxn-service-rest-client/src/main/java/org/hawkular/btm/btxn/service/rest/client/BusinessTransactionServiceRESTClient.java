/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.btxn.service.rest.client;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hawkular.btm.api.log.MsgLogger;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the REST client implementation for the Business Transaction Service
 * API.
 *
 * @author gbrown
 */
public class BusinessTransactionServiceRESTClient implements BusinessTransactionService {

    private static final MsgLogger msgLog = MsgLogger.LOGGER;

    private static final Logger log = Logger.getLogger(BusinessTransactionServiceRESTClient.class);

    private static final TypeReference<java.util.List<BusinessTransaction>> BUSINESS_TXN_LIST =
            new TypeReference<java.util.List<BusinessTransaction>>() {
            };

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String HAWKULAR_PERSONA = "Hawkular-Persona";

    private static Client client = ClientBuilder.newClient();

    private String username = System.getProperty("hawkular-btm.username");
    private String password = System.getProperty("hawkular-btm.password");

    private String authorization = null;

    private String baseUrl = System.getProperty("hawkular-btm.base-uri");

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
    }

    /**
     * @return the baseUrl
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl the baseUrl to set
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#store(java.lang.String, java.util.List)
     */
    @Override
    public void store(String tenantId, List<BusinessTransaction> btxns) throws Exception {
        String json = mapper.writeValueAsString(btxns);
        Response resp = getTarget(tenantId, "transactions", null).post(Entity.json(json));

        try {
            if (resp.getStatus() != Status.OK.getStatusCode()) {
                log.debugf("Failed to store business transactions: status=[%s]", resp.getStatusInfo());
                throw new Exception(resp.getStatusInfo().getReasonPhrase());
            }
        } finally {
            resp.close();
        }
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#get(java.lang.String, java.lang.String)
     */
    @Override
    public BusinessTransaction get(String tenantId, String id) {
        log.tracef("Get business transaction: tenantId=[%s] id=[%s]", tenantId, id);

        Response resp = getTarget(tenantId, "transactions/" + id, null).get();

        try {
            if (resp.getStatus() == Status.OK.getStatusCode()) {
                String json = resp.readEntity(String.class);

                log.tracef("Returned json=[%s]", json);

                try {
                    return mapper.readValue(json.getBytes(), BusinessTransaction.class);
                } catch (Throwable t) {
                    msgLog.errorFailedToDeserializeJson(json, t);
                }
            }
        } finally {
            resp.close();
        }

        log.debugf("Failed to get business transaction: status=[%s]", resp.getStatusInfo());

        return null;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.BusinessTransactionService#query(java.lang.String,
     *                      org.hawkular.btm.api.services.BusinessTransactionCriteria)
     */
    @Override
    public List<BusinessTransaction> query(String tenantId, BusinessTransactionCriteria criteria) {
        log.tracef("Get business transactions: tenantId=[%s] query=[%s]", tenantId, criteria);

        Response resp = getTarget(tenantId, "transactions", getQueryParameters(criteria)).get();

        try {
            if (resp.getStatus() == Status.OK.getStatusCode()) {
                String json = resp.readEntity(String.class);

                try {
                    return mapper.readValue(json.getBytes(),
                            BUSINESS_TXN_LIST);
                } catch (Exception e) {
                    msgLog.errorFailedToDeserializeJson(json, e);
                }
            }
        } finally {
            resp.close();
        }

        log.debugf("Failed to query business transaction: status=[%s]", resp.getStatusInfo());

        return null;
    }

    /**
     * Create a builder, based on the supplied path, with an
     * optional tenantId header value.
     *
     * @param tenantId The optional tenant id
     * @param path The path
     * @param queryParameters The optional query parameters
     * @return The builder
     */
    protected Builder getTarget(String tenantId, String path, Map<String, String> queryParameters) {
        WebTarget target = client.target(baseUrl).path(path);

        if (queryParameters != null && !queryParameters.isEmpty()) {
            for (String key : queryParameters.keySet()) {
                target.queryParam(key, queryParameters.get(key));
            }
        }

        Builder builder = target.request();

        if (tenantId != null) {
            builder = builder.header(HAWKULAR_PERSONA, tenantId);
        }

        if (authorization == null && username != null) {
            String authString = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(authString.getBytes());

            authorization = "Basic " + encoded;
        }

        if (authorization != null) {
            builder = builder.header("Authorization", authorization);
        }

        return builder;
    }

    /**
     * This method returns a query string representing the criteria
     * specified. If a blank criteria is specified, then an empty
     * string will be returned, otherwise the relevant query
     * parameters/values will be defined following an initial '?'
     * character.
     *
     * @param criteria The business transaction criteria
     * @return The query parameters
     */
    protected static Map<String, String> getQueryParameters(BusinessTransactionCriteria criteria) {
        Map<String, String> ret = new HashMap<String, String>();

        if (criteria.getStartTime() > 0) {
            ret.put("startTime", "" + criteria.getStartTime());
        }

        if (criteria.getEndTime() > 0) {
            ret.put("endTime", "" + criteria.getEndTime());
        }

        if (!criteria.getProperties().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (String key : criteria.getProperties().keySet()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(key);
                buf.append('|');
                buf.append(criteria.getProperties().get(key));
            }

            ret.put("properties", buf.toString());
        }

        if (!criteria.getCorrelationIds().isEmpty()) {
            boolean first = true;
            StringBuilder buf = new StringBuilder();

            for (CorrelationIdentifier cid : criteria.getCorrelationIds()) {
                if (first) {
                    first = false;
                } else {
                    buf.append(',');
                }
                buf.append(cid.getScope().name());
                buf.append('|');
                buf.append(cid.getValue());
            }

            ret.put("correlations", buf.toString());
        }

        return ret;
    }
}
