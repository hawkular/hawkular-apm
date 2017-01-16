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
package org.hawkular.apm.processor.alerts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hawkular.apm.api.model.Property;

/**
 * @author Juraci Paixão Kröhling
 */
public class Event {
    private String category;
    private Map<String, String> context = new HashMap<>(1);
    private Map<String, String> tags = new HashMap<>();
    private long ctime;
    private String id;
    private String dataId;
    private String dataSource;
    private String text;

    public Event() {
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

    public void initTagsFromProperties(Set<Property> properties) {
        if (properties != null && properties.size() > 0) {
            properties.stream().filter(p -> p.getValue() != null).forEach(p -> {
                String value = getTags().get(p.getName());
                getTags().put(p.getName(), value == null ? p.getValue()
                        : String.format("%s,%s", value, p.getValue()));
            });
        }
    }

    @Override
    public String toString() {
        return "Event [category=" + category + ", context=" + context + ", tags=" + tags + ", ctime=" + ctime + ", id="
                + id + ", dataId=" + dataId + ", dataSource=" + dataSource + ", text=" + text + "]";
    }

}
