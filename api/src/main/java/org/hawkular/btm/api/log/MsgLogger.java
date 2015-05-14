/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Log for INFO, WARN, ERROR and FATAL messages.
 *
 * @author gbrown
 */
@MessageLogger(projectCode = "HAWKBTM")
@ValidIdRange(min = 600000, max = 609999)
public interface MsgLogger extends BasicLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 600000, value = "Failed to deserialize json [%s]")
    void errorFailedToDeserializeJson(String json, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 600001, value = "Failed to serialize json")
    void errorFailedToSerializeToJson(@Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 600002, value = "Failed to convert property [%s] to type [%s]")
    void errorConvertingPropertyToType(String property, String targetType, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 600003, value = "Failed to send a message")
    void errorSendingMessage(@Cause Throwable t);

}
