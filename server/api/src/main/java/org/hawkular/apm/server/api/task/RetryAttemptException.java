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
package org.hawkular.apm.server.api.task;

/**
 * This exception class indicates that a failure occurred while processing one or
 * more events, and therefore a retry should be performed.
 *
 * @author gbrown
 */
public class RetryAttemptException extends Exception {

    private static final long serialVersionUID = -9183048971847453822L;

    /**
     * This constructor initialises the message.
     *
     * @param mesg The message
     */
    public RetryAttemptException(String mesg) {
        super(mesg);
    }

    /**
     * This constructor initialises the associated exception.
     *
     * @param t The associated exception
     */
    public RetryAttemptException(Throwable t) {
        super(t);
    }

    /**
     * This constructor initialises the message and associated exception.
     *
     * @param mesg The message
     * @param t The associated exception
     */
    public RetryAttemptException(String mesg, Throwable t) {
        super(mesg, t);
    }
}
