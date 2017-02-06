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
package org.hawkular.apm.server.api.model.zipkin;

import java.io.Serializable;

/**
 * Binary annotations are tags applied to a Span to give it context. For
 * example, a binary annotation of "http.url" could be the path to a resource in a
 * RPC call.
 *
 * Binary annotations of type STRING are always queryable, though more a
 * historical implementation detail than a structural concern.
 *
 * Binary annotations can repeat, and vary on the host. Similar to Annotation,
 * the host indicates who logged the event. This allows you to tell the
 * difference between the client and server side of the same key. For example,
 * the key "http.url" might be different on the client and server side due to
 * rewriting, like "/api/v1/myresource" vs "/myresource. Via the host field,
 * you can see the different points of view, which often help in debugging.
 */
public class BinaryAnnotation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;

    private String value;

    private AnnotationType type;

    private Endpoint endpoint;

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the type
     */
    public AnnotationType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(AnnotationType type) {
        this.type = type;
    }

    /**
     * @return the endpoint
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint the endpoint to set
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String toString() {
        return "BinaryAnnotation [key=" + key + ", value=" + value + ", type=" + type + ", endpoint=" + endpoint + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BinaryAnnotation)) return false;

        BinaryAnnotation that = (BinaryAnnotation) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        if (type != that.type) return false;
        return endpoint != null ? endpoint.equals(that.endpoint) : that.endpoint == null;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        return result;
    }
}
