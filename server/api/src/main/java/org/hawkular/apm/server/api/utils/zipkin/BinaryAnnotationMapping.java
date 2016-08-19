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

package org.hawkular.apm.server.api.utils.zipkin;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.events.ProducerInfo;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Producer;
import org.hawkular.apm.api.model.trace.Trace;

/**
 * Zipkin binary annotations hold various information which can be used to enrich APM data model (e.g. Trace and its
 * derived objects). Therefore this mapping is used to map binary annotation key to these properties of these objects.
 *
 * @author Pavol Loffay
 */
public class BinaryAnnotationMapping {

    /**
     * Mapping for component type {@link Component#componentType}
     */
    private String componentType;
    /**
     * Mapping for endpoint type is {@link Producer#endpointType} and @see {@link Consumer#endpointType}.
     */
    private String endpointType;
    /**
     * Mapping for details map {@link Node#details} and {@link NodeDetails#details}.
     */
    private ExcludableProperty nodeDetails;
    /**
     * Mapping for property {@link Trace#properties}, and its derived objects e.g.
     * {@link CommunicationDetails#properties},
     * {@link CompletionTime#properties},
     * {@link NodeDetails#properties},
     * {@link ProducerInfo#properties}.
     */
    private IncludableProperty property;

    /**
     * Binary annotation is ignored
     */
    private boolean ignore;

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getEndpointType() {
        return endpointType;
    }

    public void setEndpointType(String endpointType) {
        this.endpointType = endpointType;
    }

    public ExcludableProperty getNodeDetails() {
        return nodeDetails;
    }

    public void setNodeDetails(ExcludableProperty nodeDetails) {
        this.nodeDetails = nodeDetails;
    }

    public IncludableProperty getProperty() {
        return property;
    }

    public void setProperty(IncludableProperty property) {
        this.property = property;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * Represents property which is by default included
     */
    public static class ExcludableProperty {
        // ignore this property
        private boolean ignore;
        private String key;

        public boolean isIgnore() {
            return ignore;
        }

        public void setIgnore(boolean ignore) {
            this.ignore = ignore;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    /**
     * Represents property which is by default excluded
     */
    public static class IncludableProperty {
        private boolean include;
        private String key;

        public boolean isInclude() {
            return include;
        }

        public void setInclude(boolean include) {
            this.include = include;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
