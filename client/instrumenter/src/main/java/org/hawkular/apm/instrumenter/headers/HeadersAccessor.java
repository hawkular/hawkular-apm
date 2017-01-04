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
package org.hawkular.apm.instrumenter.headers;

import java.util.Map;

/**
 * This class provides a factory for obtaining header information from a target object.
 *
 * @author gbrown
 */
public interface HeadersAccessor {

    /**
     * This method returns the target type associated with this factory.
     *
     * @return The target type
     */
    String getTargetType();

    /**
     * This method returns the headers associated with the supplied target instance.
     *
     * @param target The target instance
     * @return The headers associated with the target instance
     */
    Map<String, String> getHeaders(Object target);

}
