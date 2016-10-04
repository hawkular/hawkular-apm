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
package org.hawkular.apm.client.opentracing;

import javax.inject.Singleton;

import org.hawkular.apm.client.api.reporter.TraceReporter;

import io.opentracing.AbstractAPMTracer;

/**
 * The opentracing compatible Tracer implementation for Hawkular APM.
 *
 * @author gbrown
 */
@Singleton
public class APMTracer extends AbstractAPMTracer {

    /** This constant represents the prefix used by all Hawkular APM state. */
    public static final String HAWKULAR_APM_PREFIX = "Hawkular-APM";

    /** This constant represents the interaction id exchanges between a sender and receiver. */
    public static final String HAWKULAR_APM_ID = HAWKULAR_APM_PREFIX + "-Id";

    /** This constant represents the transaction name. */
    public static final String HAWKULAR_BT_NAME = HAWKULAR_APM_PREFIX + "-BTxn";

    /** This constant represents the reporting level. */
    public static final String HAWKULAR_APM_LEVEL = HAWKULAR_APM_PREFIX + "-Level";

    /** Tag name used to represent the business transaction name */
    public static final String TRANSACTION_NAME = "transaction.name";

    public APMTracer() {
    }

    public APMTracer(TraceReporter reporter) {
        super(reporter);
    }

}
