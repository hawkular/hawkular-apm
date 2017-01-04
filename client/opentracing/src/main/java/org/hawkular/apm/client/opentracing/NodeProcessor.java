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

import io.opentracing.impl.APMSpan;

/**
 * This interface represents a processing capability on a span node. A default implementation
 * exists to provide the initial basic mapping, but additional processors may be supplied
 * to perform transaction specific actions. It is also possible that applications
 * may wish to register their own processors.
 *
 * @author gbrown
 */
public interface NodeProcessor {

    /**
     * This method processes the supplied span information to add information
     * to the supplied node being built.
     *
     * @param context The trace context
     * @param span The source span
     * @param nodeBuilder The target node builder
     */
    void process(TraceContext context, APMSpan span, NodeBuilder nodeBuilder);

}
