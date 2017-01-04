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
package org.hawkular.apm.api.model.config.instrumentation.jvm;

/**
 * This type represents the session action for ignoring the current node. Ignoring
 * a node only implies that if 'ignored' nodes are the only remaining nodes associated
 * with a fragment that is being captured, then the fragment will be considered
 * 'complete' - although the fragment will not be sent until all of these nodes have
 * been individually popped off the stack.
 *
 * NOTE: This is to handle situations where due to async handling of requests, some nodes
 * are processed before others. A better solution may be required, but for now this prevents
 * an EJB async invoke, providing a JAX-RS endpoint, from reporting an error as the
 * JAX-RS consumer ends before the EJB invoke (component) ends. (See HWKBTM-376 for more details).
 *
 * @author gbrown
 */
public class IgnoreNode extends SessionAction {

}
