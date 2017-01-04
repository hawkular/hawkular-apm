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
package org.hawkular.apm.client.collector;

import org.hawkular.apm.api.model.trace.Node;

/**
 * This interface represents the trace collector's
 * session manager.
 *
 * @author gbrown
 */
public interface SessionManager {

    /**
     * This method is a guard condition for Consumer based instrumentation
     * rules, used to determine whether the invoker should be permitted to
     * create and record a Consumer as part of a trace fragment.
     * If the fragment is not currently active, then the id will be check
     * first, and if defined then the function will return true, allowing
     * the Consumer node to be created. If the id is null, then the URI and
     * optional operation will be checked against any filters that have been
     * configured. If the URI and operation passes, then the instrumentation
     * rule will be permitted to proceed and create the Consumer node in the
     * trace fragment.
     *
     * @param uri The URI
     * @param operation The optional operation
     * @param id The id
     * @return Whether the fragment is already, or can be, active
     */
    boolean activate(String uri, String operation, String id);

    /**
     * This method is a guard condition for instrumentation rules, used
     * to determine whether the invoker should be permitted to create
     * and record a node as part of a trace fragment. If
     * the fragment is not currently active, then the URI and optional
     * operation will be checked against any filters that have been configured.
     * If it passes, then the instrumentation rule will be permitted to
     * proceed and create the appropriate node in the trace fragment.
     *
     * @param uri The URI
     * @param operation The optional operation
     * @return Whether the fragment is already, or can be, active
     */
    boolean activate(String uri, String operation);

    /**
     * This method determines if there is an active session associated with
     * this thread of execution.
     *
     * @return Whether the current thread of execution has an active session
     */
    boolean isActive();

    /**
     * This method deactivates an active session. This will result in the current
     * fragment builder being discarded.
     */
    void deactivate();

    /**
     * This method indicates that the current node, for this thread of execution, should
     * be retained temporarily pending further changes. IMPORTANT: Make sure the node
     * is released, as otherwise this will prevent the fragment from being completed
     * (and therefore reported).
     *
     * @param id The identifier used to later on to identify the node
     */
    void retainNode(String id);

    /**
     * This method indicates that the identified node, for this thread of execution, should
     * be released. IMPORTANT: It is important that any previously retained node is released
     * before the trace fragment can be considered complete and therefore
     * reported.
     *
     * @param id The identifier used to identify the node
     */
    void releaseNode(String id);

    /**
     * This method returns the node associated, for this thread of execution, identified
     * by the supplied id.
     *
     * @param id The identifier used to identify the node
     * @return The node, or null if not found
     */
    Node retrieveNode(String id);

    /**
     * This method initiates a correlation between this thread of execution and one or more based
     * on the supplied id.
     *
     * @param id The id
     */
    void initiateCorrelation(String id);

    /**
     * This method identifies whether a correlation with the supplied id is currently active
     * (i.e. awaiting completion).
     *
     * @param id The id
     * @return Whether the correlation is active
     */
    boolean isCorrelated(String id);

    /**
     * This method associates the current thread of execution with the session associated
     * with the supplied correlation id.
     *
     * @param id The id associated with the target thread of execution
     */
    void correlate(String id);

    /**
     * This method ends the correlation between the current thread of execution and another
     * associated with the supplied id.
     *
     * @param id The id associated with the target thread of execution
     * @param allowSpawn Determine if completing correlation can spawn fragment if required
     */
    void completeCorrelation(String id, boolean allowSpawn);

    /**
     * Unlink the current "linked" thread of execution from the target thread.
     */
    void unlink();

    /**
     * This method suppressed recording of any child nodes under the current
     * transaction fragment node.
     */
    void suppress();

    /**
     * This method indicates that the current node can be ignored if handled out of
     * order.
     */
    void ignoreNode();

    /**
     * This method asserts that the current thread of execution is complete. It has no
     * impact on trace reporting, but is used as a sanity check to ensure
     * the collection is working correctly.
     */
    void assertComplete();

    /**
     * This method stores state information associated with the name and optional
     * context.
     *
     * @param context The optional context
     * @param name The name
     * @param value The value
     * @param session Whether related to session
     */
    void setState(Object context, String name, Object value, boolean session);

    /**
     * This method returns the state associated with the name and optional
     * context.
     *
     * @param context The optional context
     * @param name The name
     * @param session Whether related to session
     * @return The state, or null if not found
     */
    Object getState(Object context, String name, boolean session);

}
