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
package org.hawkular.btm.api.model.analytics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents information related to URIs associated with
 * unbound business transaction fragments.
 *
 * @author gbrown
 */
public class URIInfo {

    @JsonInclude
    private String uri;

    @JsonInclude
    private String endpointType;

    @JsonInclude(Include.NON_EMPTY)
    private String regex;

    @JsonInclude(Include.NON_EMPTY)
    private String template;

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     * @return the URI info
     */
    public URIInfo setUri(String uri) {
        this.uri = uri;
        return this;
    }

    /**
     * @return the endpointType
     */
    public String getEndpointType() {
        return endpointType;
    }

    /**
     * @param endpointType the endpointType to set
     * @return the URI info
     */
    public URIInfo setEndpointType(String endpointType) {
        this.endpointType = endpointType;
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
        return uri != null && (uri.indexOf("/*/") != -1 || uri.endsWith("/*"));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "URIInfo [uri=" + uri + ", endpointType=" + endpointType + ", regex=" + regex + ", template="
                + template + "]";
    }

}
