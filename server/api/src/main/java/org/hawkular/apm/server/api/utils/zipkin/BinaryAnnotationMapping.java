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

package org.hawkular.apm.server.api.utils.zipkin;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.events.NodeDetails;
import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
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
     * Mapping for property {@link Trace#allProperties()}, and its derived objects e.g.
     * {@link CommunicationDetails#properties},
     * {@link CompletionTime#properties},
     * {@link NodeDetails#properties},
     * {@link SourceInfo#properties}.
     */
    private ExcludableProperty property;

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

    public ExcludableProperty getProperty() {
        return property;
    }

    public void setProperty(ExcludableProperty property) {
        this.property = property;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    /**
     * Represents property which is by default excluded
     */
    public static class ExcludableProperty {
        private boolean exclude;
        private String key;

        public boolean isExclude() {
            return exclude;
        }

        public void setExclude(boolean exclude) {
            this.exclude = exclude;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
