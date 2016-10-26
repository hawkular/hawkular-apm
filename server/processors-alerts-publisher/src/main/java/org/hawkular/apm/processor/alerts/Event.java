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
package org.hawkular.apm.processor.alerts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;

/**
 * @author Juraci Paixão Kröhling
 */
public class Event {
    private String category;
    private Map<String, String> context;
    private Map<String, String> tags;
    private long ctime;
    private String id;
    private String dataId;
    private String dataSource;
    private String text;

    public Event() {
    }

    public Event(CompletionTime completionTime, String eventSource) {
        if (null == eventSource || eventSource.isEmpty()) {
            // TODO: should we instead just degrade the info we have? Like, by putting a generic value?
            throw new IllegalStateException("Cannot determine the event source");
        }
        this.context = new HashMap<>(1);
        this.context.put("id", completionTime.getId());

        this.tags = new HashMap<>();
        if (null != completionTime.getUri()) {
            this.tags.put("uri", completionTime.getUri());
        }
        if (null != completionTime.getOperation()) {
            this.tags.put("operation", completionTime.getOperation());
        }

        Set<Property> properties = completionTime.getProperties();
        if (properties != null && properties.size() > 0) {
            properties.forEach(p -> {
                String value = this.tags.get(p.getName());
                this.tags.put(p.getName(), value == null ? p.getValue()
                        : String.format("%s,%s", value, p.getValue()));
            });
        }

        this.dataId = eventSource;
        this.category = "APM";
        this.dataSource = completionTime.getHostName();
        this.id = UUID.randomUUID().toString();
        this.ctime = completionTime.getTimestamp();
        this.text = Long.toString(completionTime.getDuration());
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Event [category=" + category + ", context=" + context + ", tags=" + tags + ", ctime=" + ctime + ", id="
                + id + ", dataId=" + dataId + ", dataSource=" + dataSource + ", text=" + text + "]";
    }

}
