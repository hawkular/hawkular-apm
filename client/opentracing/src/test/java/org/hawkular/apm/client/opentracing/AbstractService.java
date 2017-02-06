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
package org.hawkular.apm.client.opentracing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.opentracing.Tracer;

/**
 * @author gbrown
 */
public class AbstractService {

    private Tracer tracer;

    private List<Message> outboundMessages = new ArrayList<>();

    public AbstractService(Tracer tracer) {
        this.tracer = tracer;
    }

    protected Tracer getTracer() {
        return tracer;
    }

    protected Message createMessage() {
        Message message = new Message();
        outboundMessages.add(message);
        return message;
    }

    public List<Message> getMessages() {
        return Collections.unmodifiableList(outboundMessages);
    }

    protected void delay(long interval) {
        synchronized (this) {
            try {
                wait(interval);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
