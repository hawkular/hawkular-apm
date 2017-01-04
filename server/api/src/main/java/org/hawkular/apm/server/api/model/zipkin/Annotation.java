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
 * An annotation is similar to a log statement. It includes a host field which
 * allows these events to be attributed properly, and also aggregatable.
 */
public class Annotation implements Serializable {

    private static final long serialVersionUID = 1L;

    private long timestamp;

    private String value;

    /**
     * Always the host that recorded the event. By specifying the host you allow
     * rollup of all events (such as client requests to a service) by IP address.
     */
    private Endpoint endpoint;

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        return "Annotation [timestamp=" + timestamp + ", value=" + value + ", endpoint=" + endpoint + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Annotation)) return false;

        Annotation that = (Annotation) o;

        if (timestamp != that.timestamp) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;
        return endpoint != null ? endpoint.equals(that.endpoint) : that.endpoint == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        return result;
    }
}
