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
package org.hawkular.apm.api.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

/**
 * This class provides access to properties.
 *
 * @author gbrown
 */
public class PropertyUtil {

    private static final Logger LOG = Logger.getLogger(PropertyUtil.class.getName());

    public static final String HAWKULAR_TENANT = "HAWKULAR_TENANT";

    /**
     * Location of the Hawkular Services
     */
    public static final String HAWKULAR_URI = "HAWKULAR_URI";

    /**
     * Username for authentication against Hawkular Services
     */
    public static final String HAWKULAR_USERNAME = "HAWKULAR_USERNAME";

    /**
     * Password for authentication against Hawkular Services
     */
    public static final String HAWKULAR_PASSWORD = "HAWKULAR_PASSWORD";

    /**
     * The location of the APM server
     */
    public static final String HAWKULAR_APM_URI = "HAWKULAR_APM_URI";

    /**
     * If specified, the value for this property will override the HAWKULAR_APM_URI value
     * when accessing the services.
     */
    public static final String HAWKULAR_APM_URI_SERVICES = "HAWKULAR_APM_URI_SERVICES";

    /**
     * If specified, the value for this property will override the HAWKULAR_APM_URI value
     * when publishing trace fragments.
     */
    public static final String HAWKULAR_APM_URI_PUBLISHER = "HAWKULAR_APM_URI_PUBLISHER";

    /**
     * The username to use for accessing the APM server.
     */
    public static final String HAWKULAR_APM_USERNAME = "HAWKULAR_APM_USERNAME";

    /**
     * The password to use for accessing the APM server.
     */
    public static final String HAWKULAR_APM_PASSWORD = "HAWKULAR_APM_PASSWORD";

    /**
     * The retry interval used by an application attempting to obtain the collector configuration.
     */
    public static final String HAWKULAR_APM_CONFIG_RETRY_INTERVAL = "HAWKULAR_APM_CONFIG_RETRY_INTERVAL";

    /**
     * The refresh interval to check for updates to the transaction configuration.
     */
    public static final String HAWKULAR_APM_CONFIG_REFRESH = "HAWKULAR_APM_CONFIG_REFRESH";

    /**
     * Client side logging level.
     */
    public static final String HAWKULAR_APM_LOG_LEVEL = "HAWKULAR_APM_LOG_LEVEL";

    /**
     * Client side boolean property to indicate whether to log to java.util.logging.
     */
    public static final String HAWKULAR_APM_LOG_JUL = "HAWKULAR_APM_LOG_JUL";

    /**
     * The maximum number of traces to batch before sending to the server.
     */
    public static final String HAWKULAR_APM_COLLECTOR_BATCHSIZE = "HAWKULAR_APM_COLLECTOR_BATCHSIZE";

    /**
     * The maximum time (in milliseconds) before sending a batch of traces to the server.
     */
    public static final String HAWKULAR_APM_COLLECTOR_BATCHTIME = "HAWKULAR_APM_COLLECTOR_BATCHTIME";

    /**
     * The thread pool size for reporting a batch of traces to the server.
     */
    public static final String HAWKULAR_APM_COLLECTOR_BATCHTHREADS = "HAWKULAR_APM_COLLECTOR_BATCHTHREADS";

    /**
     * The time interval (in milliseconds) between checks for expired trace states in the agent.
     */
    public static final String HAWKULAR_APM_AGENT_STATE_EXPIRY_INTERVAL = "HAWKULAR_APM_AGENT_STATE_EXPIRY_INTERVAL";

    /**
     * Comma separated list of whitelisted file extensions. All others will be ignored by the agent.
     */
    public static final String HAWKULAR_APM_AGENT_FILE_EXTENSION_WHITELIST = "HAWKULAR_APM_AGENT_FILE_EXTENSION_WHITELIST";

    /**
     * The maximum number of retry attempts when processing a batch of events.
     */
    public static final String HAWKULAR_APM_PROCESSOR_MAX_RETRY_COUNT = "HAWKULAR_APM_PROCESSOR_MAX_RETRY_COUNT";

    /**
     * The interval between retrying the processing of a batch of failed events.
     */
    public static final String HAWKULAR_APM_PROCESSOR_RETRY_DELAY = "HAWKULAR_APM_PROCESSOR_RETRY_DELAY";

    /**
     * The interval between retrying the processing of a batch of failed events for the last time before
     * giving up.
     */
    public static final String HAWKULAR_APM_PROCESSOR_LAST_RETRY_DELAY = "HAWKULAR_APM_PROCESSOR_LAST_RETRY_DELAY";

    /**
     * The standard polling interval (in milliseconds) used by processors.
     */
    public static final String HAWKULAR_APM_KAFKA_POLLING_INTERVAL = "HAWKULAR_APM_KAFKA_POLLING_INTERVAL";

    /**
     * The maximum number of records to retrieve when polling a topic.
     */
    public static final String HAWKULAR_APM_KAFKA_MAX_POLL_RECORDS = "HAWKULAR_APM_KAFKA_MAX_POLL_RECORDS";

    /**
     * The maximum number of retries when a producer send fails.
     */
    public static final String HAWKULAR_APM_KAFKA_PRODUCER_RETRIES = "HAWKULAR_APM_KAFKA_PRODUCER_RETRIES";

    /**
     * The interval (in milliseconds) between consumer auto commits.
     */
    public static final String HAWKULAR_APM_KAFKA_CONSUMER_AUTO_COMMIT_INTERVAL =
                                    "HAWKULAR_APM_KAFKA_CONSUMER_AUTO_COMMIT_INTERVAL";

    /**
     * The kafka consumer session timeout (in milliseconds).
     */
    public static final String HAWKULAR_APM_KAFKA_CONSUMER_SESSION_TIMEOUT =
                                    "HAWKULAR_APM_KAFKA_CONSUMER_SESSION_TIMEOUT";

    /**
     * List of allowed CORS origins.
     */
    public static final String HAWKULAR_APM_CORS_ALLOWED_ORIGINS = "HAWKULAR_APM_CORS_ALLOWED_ORIGINS";

    /**
     * List of extra CORS Access-Control-Allow-Headers, this list is added to predefined list of allowed headers.
     */
    public static final String HAWKULAR_APM_CORS_ACCESS_CONTROL_ALLOW_HEADERS =
            "HAWKULAR_APM_CORS_ACCESS_CONTROL_ALLOW_HEADERS";

    /** The URI prefix to denote use of Kafka */
    public static final String KAFKA_PREFIX = "kafka:";

    /**
     * Environment variable representing the container's host name.
     */
    private static final String ENV_HOSTNAME = "HOSTNAME";

    /**
     * Property specifying the service name, to be used in a client-side library
     */
    public static final String HAWKULAR_APM_SERVICE_NAME = "HAWKULAR_APM_SERVICE_NAME";

    /**
     * Property specifying the build stamp (or service version), to be used in a client-side library
     */
    public static final String HAWKULAR_APM_BUILDSTAMP = "HAWKULAR_APM_BUILDSTAMP";

    /**
     * Property specifying the env var name for the OpenShift build name.
     */
    public static final String OPENSHIFT_BUILD_NAME = "OPENSHIFT_BUILD_NAME";

    /**
     * Property specifying the env var name for the OpenShift build namespace.
     */
    public static final String OPENSHIFT_BUILD_NAMESPACE = "OPENSHIFT_BUILD_NAMESPACE";

    /**
     * Environmental variable representing Kafka host address e.g. 127.0.0.1:2181.
     * If the port is not specified it will use default 2181.
     */
    private static final String ENV_KAFKA_ZOOKEEPER = "KAFKA_ZOOKEEPER";

    private static String hostName;
    private static String hostAddress;

    static {
        hostName = System.getenv(ENV_HOSTNAME);
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.severe("Unable to determine host name");
            }
        }

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOG.severe("Unable to determine host address");
        }
    }

    /**
     * This method determines whether a property exists, as a system property
     * or environment variable.
     *
     * @param name The name
     * @return Whether the property has been defined
     */
    public static boolean hasProperty(String name) {
        return getProperty(name) != null;
    }

    /**
     * This method returns the named property, first checking the system properties
     * and if not found, checking the environment.
     *
     * @param name The name
     * @return The property, or null if not found
     */
    public static String getProperty(String name) {
        return System.getProperty(name, System.getenv(name));
    }

    /**
     * This method returns the named property, first checking the system properties
     * and if not found, checking the environment.
     *
     * @param name The name
     * @param def The default if not found
     * @return The property, or the default value if not found
     */
    public static String getProperty(String name, String def) {
        String ret = System.getProperty(name, System.getenv(name));
        if (ret != null) {
            return ret;
        }
        return def;
    }

    /**
     * This method returns the property as an integer value.
     *
     * @param name The property name
     * @return The property as an integer, or null if not found
     */
    public static Integer getPropertyAsInteger(String name) {
        return getPropertyAsInteger(name, null);
    }

    /**
     * This method returns the property as an integer value.
     *
     * @param name The property name
     * @param def The optional default value
     * @return The property as an integer, or null if not found
     */
    public static Integer getPropertyAsInteger(String name, Integer def) {
        String value = getProperty(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Failed to convert property value '" + value + "' to integer", e);
            }
        }
        return def;
    }

    /**
     * This method returns the property as an integer value.
     *
     * @return The property as a boolean, or false if not found
     */
    public static boolean getPropertyAsBoolean(String name) {
        String value = getProperty(name);
        if (value != null && value.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    /**
     * This method returns the hostname on which the application is running.
     *
     * @return The host name
     */
    public static String getHostName() {
        return hostName;
    }

    /**
     * This method returns the host IP address on which the application is running.
     *
     * @return The host IP address
     */
    public static String getHostAddress() {
        return hostAddress;
    }

    /**
     * See {@link #ENV_KAFKA_ZOOKEEPER};
     */
    public static String getKafkaZookeeper() {
        return System.getenv(ENV_KAFKA_ZOOKEEPER);
    }
}
