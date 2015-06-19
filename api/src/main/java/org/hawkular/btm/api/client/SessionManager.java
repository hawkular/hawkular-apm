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
package org.hawkular.btm.api.client;

import org.hawkular.btm.api.model.btxn.Node;

/**
 * This interface represents the business transaction collector's
 * session manager.
 *
 * @author gbrown
 */
public interface SessionManager {

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
     * before the business transaction fragment can be considered complete and therefore
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
     * This method initiates a link between this thread of execution and another one based
     * on the supplied id.
     *
     * @param id The id
     */
    void initiateLink(String id);

    /**
     * This method completes the link between the current thread of execution and another
     * associated with the supplied id. The association with the target thread will be
     * maintained until either the target thread completes, or this method is called again
     * with a different id.
     *
     * @param id The id associated with the target thread of execution
     */
    void completeLink(String id);

    /**
     * Unlink the current "linked" thread of execution from the target thread.
     */
    void unlink();

    /**
     * This method asserts that the current thread of execution is complete. It has no
     * impact on business transaction reporting, but is used as a sanity check to ensure
     * the collection is working correctly.
     */
    void assertComplete();

}
