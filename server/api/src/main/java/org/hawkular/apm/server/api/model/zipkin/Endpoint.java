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
 * Indicates the network context of a service recording an annotation with two
 * exceptions.
 *
 * When a BinaryAnnotation, and key is CLIENT_ADDR or SERVER_ADDR,
 * the endpoint indicates the source or destination of an RPC. This exception
 * allows zipkin to display network context of uninstrumented services, or
 * clients such as web browsers.
 */
public class Endpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ipv4;

    private Short port;

    private String serviceName;

    /**
     * @return the ipv4
     */
    public String getIpv4() {
        return ipv4;
    }

    /**
     * @param ipv4 the ipv4 to set
     */
    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }

    /**
     * @return the port
     */
    public Short getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Short port) {
        this.port = port;
    }

    /**
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "ipv4='" + ipv4 + '\'' +
                ", port=" + port +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Endpoint)) return false;

        Endpoint endpoint = (Endpoint) o;

        if (ipv4 != null ? !ipv4.equals(endpoint.ipv4) : endpoint.ipv4 != null) return false;
        if (port != null ? !port.equals(endpoint.port) : endpoint.port != null) return false;
        return serviceName != null ? serviceName.equals(endpoint.serviceName) : endpoint.serviceName == null;

    }

    @Override
    public int hashCode() {
        int result = ipv4 != null ? ipv4.hashCode() : 0;
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        return result;
    }
}
