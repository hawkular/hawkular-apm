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
package org.hawkular.btm.client.collector.internal.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.config.Direction;
import org.hawkular.btm.api.model.config.btxn.ProcessorAction;
import org.hawkular.btm.api.model.config.btxn.RewriteURIAction;
import org.hawkular.btm.client.collector.internal.NodeUtil;

/**
 * This handler is associated with the RewriteURI action.
 *
 * @author gbrown
 */
public class RewriteURIActionHandler extends ProcessorActionHandler {

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public RewriteURIActionHandler(ProcessorAction action) {
        super(action);
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.client.collector.internal.actions.ProcessorActionHandler#process(
     *      org.hawkular.btm.api.model.btxn.BusinessTransaction, org.hawkular.btm.api.model.btxn.Node,
     *      org.hawkular.btm.api.model.config.Direction, java.util.Map, java.lang.Object[])
     */
    @Override
    public boolean process(BusinessTransaction btxn, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (super.process(btxn, node, direction, headers, values)) {
            if (node.getUri() != null && ((RewriteURIAction) getAction()).getTemplate() != null) {
                StringTokenizer uriTokens = new StringTokenizer(node.getUri(), "/");
                StringTokenizer templateTokens =
                        new StringTokenizer(((RewriteURIAction) getAction()).getTemplate(), "/");

                if (uriTokens.countTokens() == templateTokens.countTokens()) {
                    Map<String, String> props = null;
                    while (uriTokens.hasMoreTokens()) {
                        String uriToken = uriTokens.nextToken();
                        String templateToken = templateTokens.nextToken();

                        if (templateToken.charAt(0) == '{' && templateToken.charAt(templateToken.length() - 1) == '}') {
                            String name = templateToken.substring(1, templateToken.length() - 1);
                            if (props == null) {
                                props = new HashMap<String, String>();
                            }
                            props.put(name, uriToken);
                        } else if (!uriToken.equals(templateToken)) {
                            // URI template mismatch
                            return false;
                        }
                    }

                    // If properties extracted, then add to business txn properties, and set the node's
                    // URI to the template, to make it stable/consistent - to make analytics easier
                    if (props != null) {
                        btxn.getProperties().putAll(props);
                        NodeUtil.rewriteURI(node, ((RewriteURIAction) getAction()).getTemplate());
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
