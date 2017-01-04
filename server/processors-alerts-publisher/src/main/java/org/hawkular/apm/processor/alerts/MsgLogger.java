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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "HAWKAPM")
@ValidIdRange(min = 600200, max = 600299)
public interface MsgLogger extends BasicLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 600200, value = "Received a trace completion time information. Publishing to Hawkular Alerts.")
    void traceCompletionTimeReceived();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 600201, value = "Failed to publish completion time information to Alerts. Cause: ")
    void errorPublishingToAlerts(@Cause Throwable t);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 600202, value = "The property HAWKULAR_SERVER_URL has not being set. Skipping publishing of events to Hawkular Alerts")
    void hawkularServerNotConfigured();

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 600203, value = "The property HAWKULAR_SERVER_USERNAME has not being set. Skipping publishing of events to Hawkular Alerts")
    void hawkularServerUsernameNotConfigured();

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 600204, value = "The property HAWKULAR_SERVER_PASSWORD has not being set. Skipping publishing of events to Hawkular Alerts")
    void hawkularServerPasswordNotConfigured();

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 600205, value = "Received a invocation details. Publishing to Hawkular Alerts.")
    void invocationDetailsReceived();

}
