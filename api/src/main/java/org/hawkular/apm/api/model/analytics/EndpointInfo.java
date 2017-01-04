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
package org.hawkular.apm.api.model.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents information related to endpoints associated with
 * unbound trace fragments.
 *
 * @author gbrown
 */
public class EndpointInfo {

    @JsonInclude
    private String endpoint;

    @JsonInclude
    private String type;

    @JsonInclude(Include.NON_EMPTY)
    private String regex;

    @JsonInclude(Include.NON_EMPTY)
    private String uriRegex;

    @JsonInclude(Include.NON_EMPTY)
    private String uriTemplate;

    /**
     * The default constructor.
     */
    public EndpointInfo() {
    }

    /**
     * The default constructor.
     *
     * @param endpoint The endpoint
     */
    public EndpointInfo(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * The copy constructor.
     *
     * @param endpointInfo The info to copy
     */
    public EndpointInfo(EndpointInfo endpointInfo) {
        this.endpoint = endpointInfo.endpoint;
        this.type = endpointInfo.type;
        this.regex = endpointInfo.regex;
        this.uriRegex = endpointInfo.uriRegex;
        this.uriTemplate = endpointInfo.uriTemplate;
    }

    /**
     * @return the endpoint
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     * @return the Endpoint info
     */
    public EndpointInfo setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     * @return the Endpoint info
     */
    public EndpointInfo setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * @return the regex
     */
    public String getRegex() {
        return regex;
    }

    /**
     * @param regex the regex to set
     */
    public void setRegex(String regex) {
        this.regex = regex;
    }

    /**
     * @return the uriRegex
     */
    public String getUriRegex() {
        return uriRegex;
    }

    /**
     * @param uriRegex the uriRegex to set
     */
    public void setUriRegex(String uriRegex) {
        this.uriRegex = uriRegex;
    }

    /**
     * @return the uriTemplate
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    /**
     * @param uriTemplate the uriTemplate to set
     */
    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    /**
     * This method determines if the URI represents contains meta
     * characters and therefore represents multiple real URIs.
     *
     * @return Whether this is a meta-URI
     */
    public boolean metaURI() {
        return endpoint != null && (endpoint.indexOf("/*/") != -1 || endpoint.endsWith("/*")
                || endpoint.indexOf("/*[") != -1);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        if (endpoint != null) {
            return endpoint.hashCode();
        }
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof EndpointInfo && endpoint != null) {
            return endpoint.equals(((EndpointInfo)obj).endpoint);
        }
        return false;
    }

    @Override
    public String toString() {
        return "EndpointInfo [endpoint=" + endpoint + ", type=" + type + ", regex=" + regex + ", uriRegex=" + uriRegex
                + ", uriTemplate=" + uriTemplate + "]";
    }

}
