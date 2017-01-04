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
package org.hawkular.apm.instrumenter.faults;

/**
 * This interface represents a fault descriptor, providing information about a particular
 * type of fault.
 *
 * @author gbrown
 */
public interface FaultDescriptor {

    /**
     * This method determines if the supplied fault is associated
     * with the descriptor.
     *
     * @param fault The fault
     * @return Whether this is the descriptor for the fault
     */
    boolean isValid(Object fault);

    /**
     * This method returns the fault name.
     *
     * @param fault The fault
     * @return The name
     */
    String getName(Object fault);

    /**
     * This method returns the fault description.
     *
     * @param fault The fault
     * @return The description
     */
    String getDescription(Object fault);

}
