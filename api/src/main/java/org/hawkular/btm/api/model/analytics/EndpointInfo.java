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
package org.hawkular.btm.api.model.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents information related to endpoints associated with
 * unbound business transaction fragments.
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
    private String template;

    /**
     * The default constructor.
     */
    public EndpointInfo() {
    }

    /**
     * The copy constructor.
     *
     * @param uriInfo The info to copy
     */
    public EndpointInfo(EndpointInfo uriInfo) {
        this.endpoint = uriInfo.endpoint;
        this.type = uriInfo.type;
        this.regex = uriInfo.regex;
        this.template = uriInfo.template;
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
     * @return the template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * @param template the template to set
     */
    public void setTemplate(String template) {
        this.template = template;
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "URIInfo [endpoint=" + endpoint + ", type=" + type + ", regex=" + regex + ", template="
                + template + "]";
    }

}
