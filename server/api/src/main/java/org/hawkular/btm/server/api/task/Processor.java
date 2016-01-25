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
package org.hawkular.btm.server.api.task;

import java.util.List;

/**
 * This interface represents a processor.
 *
 * @author gbrown
 */
public interface Processor<T, R> {

    /**
     * This method determines whether the processor results in multiple results
     * per item.
     *
     * @return
     */
    boolean isMultiple();

    /**
     * This method processes the supplied item to optionally
     * generated a new resulting value.
     *
     * @param item The item
     * @return The optional value
     * @throws Exception Failed to process the item
     */
    R processSingle(T item) throws Exception;

    /**
     * This method processes the supplied item to
     * generate zero or more resulting values.
     *
     * @param item The item
     * @return The list of values
     * @throws Exception Failed to process the item
     */
    List<R> processMultiple(T item) throws Exception;

}
