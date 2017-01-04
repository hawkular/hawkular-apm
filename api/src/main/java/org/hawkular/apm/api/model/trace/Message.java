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
package org.hawkular.apm.api.model.trace;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents information exchanged between two participants in the
 * transaction flow.
 *
 * @author gbrown
 *
 */
public class Message {

    @JsonInclude(Include.NON_EMPTY)
    private Map<String, String> headers = new HashMap<String, String>();

    @JsonInclude
    private Map<String, Content> content = new HashMap<String, Content>();

    public Message() {
    }

    /**
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @param headers the headers to set
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * @return the content
     */
    public Map<String, Content> getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(Map<String, Content> content) {
        this.content = content;
    }

    /**
     * This method adds new content.
     *
     * @param name The optional name
     * @param type The optional type
     * @param value The value
     * @return This message
     */
    public Message addContent(String name, String type, String value) {
        content.put(name, new Content(type, value));
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((content == null) ? 0 : content.hashCode());
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Message other = (Message) obj;
        if (content == null) {
            if (other.content != null) {
                return false;
            }
        } else if (!content.equals(other.content)) {
            return false;
        }
        if (headers == null) {
            if (other.headers != null) {
                return false;
            }
        } else if (!headers.equals(other.headers)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Message [headers=" + headers + ", content=" + content + "]";
    }

}
