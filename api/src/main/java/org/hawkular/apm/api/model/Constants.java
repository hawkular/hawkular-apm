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
package org.hawkular.apm.api.model;

/**
 * This class provides constant definitions.
 *
 * @author gbrown
 */
public class Constants {

    private Constants() {}

    /**
     * Where trace activity begins within a client application, with the
     * invocation of a URI, any information derived from that activity
     * needs to be distinguished from the data derived from the server
     * trace fragment which is also associated with the same URI. Therefore
     * this constant defines a prefix that should be added to the URI when
     * deriving information about the client activity.
     */
    public static final String URI_CLIENT_PREFIX = "client:";

    /**
     * Property key representing the service name
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_SERVICE_NAME = "service";

    /**
     * Property key representing the build stamp, like a build serial number
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_BUILD_STAMP = "buildStamp";

    /**
     * Property key representing the transaction name
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_TRANSACTION_NAME = "transaction";

    /**
     * Property key representing the principal
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_PRINCIPAL = "principal";

    /**
     * Property key representing the fault. A 'fault' will represent
     * the non-normal result of a call to a business service. For example,
     * if requesting a user's account, then a fault may be "Account Not Found".
     * Communication protocol errors will be recorded in protocol specific
     * tags, such as "http.status_code".
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_FAULT = "fault";

    /**
     * Property key representing the database statement
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_DATABASE_STATEMENT = "database.statement";

    /**
     * Property key representing the HTTP query string
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_HTTP_QUERY = "http.query";

    /**
     * Property key representing the HTTP URL template string
     * {@link org.hawkular.apm.api.model.Property#name}
     */
    public static final String PROP_HTTP_URL_TEMPLATE = "http.url_template";

    /**
     * Represents database component type of
     * {@link org.hawkular.apm.api.model.trace.Component#componentType}
     */
    public static final String COMPONENT_DATABASE = "Database";

    /**
     * Represents EJB component type of
     * {@link org.hawkular.apm.api.model.trace.Component#componentType}
     */
    public static final String COMPONENT_EJB = "EJB";


    /**
     * This constant represents the prefix used by all Hawkular APM state transferred between
     * communication services.
     */
    public static final String HAWKULAR_APM_PREFIX = "HWKAPM";

    /**
     * This constant represents the interaction id transferred between a sender and receiver
     * usually as a header property on the exchanged message.
     */
    public static final String HAWKULAR_APM_ID = HAWKULAR_APM_PREFIX + "ID";

    /**
     * This constant represents the trace id transferred between a sender and receiver
     * usually as a header property on the exchanged message.
     */
    public static final String HAWKULAR_APM_TRACEID = HAWKULAR_APM_PREFIX + "TRACEID";

    /**
     * This constant represents the transaction name transferred between a sender and receiver
     * usually as a header property on the exchanged message.
     */
    public static final String HAWKULAR_APM_TXN = HAWKULAR_APM_PREFIX + "TXN";

    /**
     * This constant represents the reporting level transferred between a sender and receiver
     * usually as a header property on the exchanged message.
     */
    public static final String HAWKULAR_APM_LEVEL = HAWKULAR_APM_PREFIX + "LEVEL";


    /**
     * Binary annotation key for HTTP URL.
     *
     * The entire URL, including the scheme, host and query parameters if available. See zipkinCoreConstants.
     */
    public static final String ZIPKIN_BIN_ANNOTATION_HTTP_URL = "http.url";

    /**
     * Binary annotation key for HTTP URL path.
     *
     * The absolute http path, without any query parameters. Ex. "/objects/abcd-ff"
     * Historical note: This was commonly expressed as "http.uri" in zipkin, eventhough it was most
     * often just a path. See zipkinCoreConstants.
     */
    public static final String ZIPKIN_BIN_ANNOTATION_HTTP_PATH = "http.path";

    /**
     * Binary annotation key for HTTP URL.
     *
     * See @{@link Constants#ZIPKIN_BIN_ANNOTATION_HTTP_PATH}.
     *
     * See zipkinCoreConstants
     */
    public static final String ZIPKIN_BIN_ANNOTATION_HTTP_URI = "http.uri";

    /**
     * The HTTP status code, when not in 2xx range. Ex. "503".  See zipkinCoreConstants.
     */
    public static final String ZIPKIN_BIN_ANNOTATION_HTTP_STATUS_CODE = "http.status_code";
}

