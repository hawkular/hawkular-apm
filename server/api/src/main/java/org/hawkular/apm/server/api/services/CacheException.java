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
package org.hawkular.apm.server.api.services;

/**
 * This class represents a cache exception.
 *
 * @author gbrown
 */
public class CacheException extends Exception {

    private static final long serialVersionUID = 4985887160445662299L;

    /**
     * This constructor initialises the exception message.
     *
     * @param mesg The message
     */
    public CacheException(String mesg) {
        super(mesg);
    }

    /**
     * This constructor initialises the associated exception.
     *
     * @param t The associated exception
     */
    public CacheException(Throwable t) {
        super(t);
    }

    /**
     * This constructor initialises the exception message.
     *
     * @param mesg The message
     * @param t The associated exception
     */
    public CacheException(String mesg, Throwable t) {
        super(mesg, t);
    }

}
