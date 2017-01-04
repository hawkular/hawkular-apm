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

package org.hawkular.apm.client.api.recorder;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.model.trace.Trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Logs entries as JSON via an underlying logger.
 *
 * @author Pavol Loffay
 */
public class LoggingRecorder implements TraceRecorder {

    private final Logger log;
    private ObjectMapper objectMapper = new ObjectMapper();

    public LoggingRecorder() {
        this(LoggingRecorder.class.getName());
    }

    public LoggingRecorder(String loggerName) {
        log = Logger.getLogger(loggerName, true);
    }

    @Override
    public void record(Trace trace) {
        if (log.isLoggable(Logger.Level.INFO)) {
            String json = serialize(trace);
            if (json != null) {
                log.info(json);
            }
        }
    }

    private String serialize(Object object) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.severe("Failed to serialize trace", e);
        }

        return json;
    }
}
