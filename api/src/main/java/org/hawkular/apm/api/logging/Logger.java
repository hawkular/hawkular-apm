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
package org.hawkular.apm.api.logging;

import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * This is a simple client side logger, to avoid using the Java util logging and JBoss logging managers.
 * If JUL is used in the javaagent, and then wildfly starts up, it complains as it cannot initialise the
 * JBoss logging manager. To avoid installing a logging manager, as with byteman, just use stdout/err.
 *
 * @author gbrown
 */
public class Logger {

    private static Level level = Level.valueOf(PropertyUtil.getProperty("HAWKULAR_APM_LOG_LEVEL", Level.INFO.name()));

    private static boolean logToJUL = Boolean.getBoolean("HAWKULAR_APM_LOG_JUL");

    private static java.util.logging.Logger logger = null;

    private String className;
    private String simpleClassName = null;

    static {
        if (logToJUL) {
            logger = java.util.logging.Logger.getLogger(Logger.class.getName());
        }
    }

    /**
     * This construct is initialised with the class name.
     *
     * @param className The class name
     */
    protected Logger(String className) {
        this.className = className;

        int index = className.lastIndexOf('.');
        if (index != -1) {
            this.simpleClassName = className.substring(index + 1);
        }
    }

    /**
     * This method returns a logger associated with the supplied class name.
     *
     * @param className The class name
     * @return The logger
     */
    public static Logger getLogger(String className) {
        return new Logger(className);
    }

    /**
     * This method determines whether the supplied logging level is loggable.
     *
     * @param l The level
     * @return Whether the level is loggable
     */
    public boolean isLoggable(Level l) {
        return l.ordinal() >= level.ordinal();
    }

    /**
     * This method logs a message at the FINEST level.
     *
     * @param mesg The message
     */
    public void finest(String mesg) {
        log(Level.FINEST, mesg, null);
    }

    /**
     * This method logs a message at the FINER level.
     *
     * @param mesg The message
     */
    public void finer(String mesg) {
        log(Level.FINER, mesg, null);
    }

    /**
     * This method logs a message at the FINE level.
     *
     * @param mesg The message
     */
    public void fine(String mesg) {
        log(Level.FINE, mesg, null);
    }

    /**
     * This method logs a message at the INFO level.
     *
     * @param mesg The message
     */
    public void info(String mesg) {
        log(Level.INFO, mesg, null);
    }

    /**
     * This method logs a message at the WARNING level.
     *
     * @param mesg The message
     */
    public void warning(String mesg) {
        log(Level.WARNING, mesg, null);
    }

    /**
     * This method logs a message at the SEVERE level.
     *
     * @param mesg The message
     */
    public void severe(String mesg) {
        log(Level.SEVERE, mesg, null);
    }

    /**
     * This method logs a message at the supplied message level
     * with an optional exception.
     *
     * @param mesgLevel The level
     * @param mesg The message
     * @param t The optional exception
     */
    public void log(Level mesgLevel, String mesg, Throwable t) {
        if (mesgLevel.ordinal() >= level.ordinal()) {
            StringBuilder builder = new StringBuilder();
            builder.append(mesgLevel.name());
            builder.append(": [");
            builder.append(simpleClassName != null ? simpleClassName : className);
            builder.append("] [");
            builder.append(Thread.currentThread());
            builder.append("] ");
            builder.append(mesg);

            if (mesgLevel == Level.SEVERE) {
                if (logger != null) {
                    logger.log(java.util.logging.Level.SEVERE, builder.toString(), t);
                } else {
                    System.err.println(builder.toString());
                }
            } else {
                if (logger != null) {
                    logger.info(builder.toString());
                } else {
                    System.out.println(builder.toString());
                }
            }

            if (t != null) {
                t.printStackTrace();
            }
        }
    }

    /**
     * This enum represents the logging levels.
     *
     * @author gbrown
     */
    public enum Level {
        FINEST,
        FINER,
        FINE,
        INFO,
        WARNING,
        SEVERE
    }
}
