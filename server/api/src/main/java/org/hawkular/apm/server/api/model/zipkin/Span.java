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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.server.api.utils.zipkin.BinaryAnnotationMappingDeriver;
import org.hawkular.apm.server.api.utils.zipkin.MappingResult;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A trace is a series of spans (often RPC calls) which form a latency tree.
 *
 * The root span is where trace_id = id and parent_id = Nil. The root span is
 * usually the longest interval in the trace, starting with a SERVER_RECV
 * annotation and ending with a SERVER_SEND.
 */
public class Span implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(Span.class.getName());

    private String traceId;

    private String name;

    private String id;

    private String parentId;

    private List<Annotation> annotations = Collections.emptyList();

    private final List<BinaryAnnotation> binaryAnnotations;

    private Boolean debug;

    private Long timestamp;

    private Long duration;

    /**
     * Hawkular APM specific
     */
    private final MappingResult mappingResult;
    private String service;
    private String ipv4;
    private URL url;

    public Span() {
        this(null, null);
    }

    @JsonCreator
    public Span(@JsonProperty("binaryAnnotations") List<BinaryAnnotation> binaryAnnotations,
                @JsonProperty("annotations") List<Annotation> annotations) {
        this.binaryAnnotations = Collections.unmodifiableList(binaryAnnotations == null ?
                Collections.emptyList() : binaryAnnotations);
        this.annotations = Collections.unmodifiableList(annotations == null ?
                Collections.emptyList() : annotations);
        this.mappingResult = BinaryAnnotationMappingDeriver.getInstance().mappingResult(binaryAnnotations);
        initUrl();
        initIpv4AndService();
    }

    /**
     * Copy construct, with ability to specify binary annotations and annotations.
     *
     * @param span span to be copied
     * @param binaryAnnotations binary annotations of the span
     * @param annotations annotations of the span
     */
    public Span(Span span, List<BinaryAnnotation> binaryAnnotations, List<Annotation> annotations) {
        this(binaryAnnotations, annotations);
        this.id = span.getId();
        this.traceId = span.getTraceId();
        this.parentId = span.getParentId();
        this.timestamp = span.getTimestamp();
        this.duration = span.getDuration();
        this.debug = span.getDebug();
        this.name = span.getName();
    }

    /**
     * @return the traceId
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * @param traceId the traceId to set
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the parentId
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * @return the annotations
     */
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * @return the binaryAnnotations
     */
    public List<BinaryAnnotation> getBinaryAnnotations() {
        return binaryAnnotations;
    }

    /**
     * @return the debug
     */
    public Boolean getDebug() {
        return debug;
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    /**
     * @return the timestamp
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the duration
     */
    public Long getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public boolean topLevelSpan() {
        return getParentId() == null || getParentId().equals(getId());
    }

    public boolean clientSpan() {
        boolean csPresent = false;
        boolean crPresent = false;
        for (int i = 0; i < annotations.size(); i++) {
            if (annotations.get(i).getValue().equals("cs")) {
                csPresent = true;
            } else if (annotations.get(i).getValue().equals("cr")) {
                crPresent = true;
            }

            if (csPresent && crPresent) {
                break;
            }
        }

        return csPresent && crPresent;
    }

    public boolean serverSpan() {
        boolean srPresent = false;
        boolean ssPresent = false;
        for (int i = 0; i < annotations.size(); i++) {
            if (annotations.get(i).getValue().equals("sr")) {
                srPresent = true;
            } else if (annotations.get(i).getValue().equals("ss")) {
                ssPresent = true;
            }

            if (srPresent && ssPresent) {
                break;
            }
        }

        return srPresent && ssPresent;
    }

    public BinaryAnnotation getBinaryAnnotation(String key) {
        for (int i=0; i < binaryAnnotations.size(); i++) {
            BinaryAnnotation ba = binaryAnnotations.get(i);
            if (ba.getKey().equals(key)) {
                return ba;
            }
        }
        return null;
    }

    public MappingResult binaryAnnotationMapping() {
        return mappingResult;
    }

    public String ipv4() {
        return ipv4;
    }

    public String service() {
        return service;
    }

    public URL url() {
        return url;
    }

    private void initUrl() {
        BinaryAnnotation httpUrl = getBinaryAnnotation(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URL);
        if (httpUrl == null) {
            httpUrl = getBinaryAnnotation(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_URI);
        }
        if (httpUrl == null) {
            httpUrl = getBinaryAnnotation(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_PATH);
        }
        if (httpUrl != null) {
            try {
                url = new URL(httpUrl.getValue());
            } catch (MalformedURLException e) {
                // Use the value as a path
                try {
                    url = new URL("http", null, httpUrl.getValue());
                } catch (MalformedURLException e1) {
                    log.log(Level.SEVERE, "Failed to decode URL", e);
                }
            }
        }
    }

    private void initIpv4AndService() {
        if (annotations == null) {
            return;
        }

        Set<Endpoint> endpoints = annotations.stream()
                .filter(annotation -> annotation.getEndpoint() != null)
                .map(Annotation::getEndpoint)
                .collect(Collectors.toSet());

        if (endpoints.size() > 1) {
            log.finest("Multiple different Endpoints within one Span: " + endpoints);
        }

        Endpoint endpoint = endpoints.size() > 0 ? endpoints.iterator().next() : null;
        if (endpoint != null) {
            ipv4 = endpoint.getIpv4();
            service = endpoint.getServiceName();
        }
    }

    @Override
    public String toString() {
        return "Span [traceId=" + traceId + ", name=" + name + ", id=" + id + ", parentId=" + parentId
                + ", annotations=" + annotations + ", binaryAnnotations=" + binaryAnnotations + ", debug=" + debug
                + ", timestamp=" + timestamp + ", duration=" + duration + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Span)) return false;

        Span span = (Span) o;

        if (traceId != null ? !traceId.equals(span.traceId) : span.traceId != null) return false;
        if (name != null ? !name.equals(span.name) : span.name != null) return false;
        if (id != null ? !id.equals(span.id) : span.id != null) return false;
        if (parentId != null ? !parentId.equals(span.parentId) : span.parentId != null) return false;
        if (annotations != null ? !annotations.equals(span.annotations) : span.annotations != null) return false;
        if (binaryAnnotations != null ? !binaryAnnotations.equals(span.binaryAnnotations) :
                span.binaryAnnotations != null)
            return false;
        if (debug != null ? !debug.equals(span.debug) : span.debug != null) return false;
        if (timestamp != null ? !timestamp.equals(span.timestamp) : span.timestamp != null) return false;
        if (duration != null ? !duration.equals(span.duration) : span.duration != null) return false;
        if (mappingResult != null ? !mappingResult.equals(span.mappingResult) : span.mappingResult != null)
            return false;
        if (service != null ? !service.equals(span.service) : span.service != null) return false;
        if (ipv4 != null ? !ipv4.equals(span.ipv4) : span.ipv4 != null) return false;
        return url != null ? url.equals(span.url) : span.url == null;

    }

    @Override
    public int hashCode() {
        int result = traceId != null ? traceId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
        result = 31 * result + (annotations != null ? annotations.hashCode() : 0);
        result = 31 * result + (binaryAnnotations != null ? binaryAnnotations.hashCode() : 0);
        result = 31 * result + (debug != null ? debug.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (mappingResult != null ? mappingResult.hashCode() : 0);
        result = 31 * result + (service != null ? service.hashCode() : 0);
        result = 31 * result + (ipv4 != null ? ipv4.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
