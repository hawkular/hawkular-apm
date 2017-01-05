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
package org.hawkular.apm.api.internal.actions;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.Severity;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.ConfigMessage;
import org.hawkular.apm.api.model.config.txn.EvaluateURIAction;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.ProcessorAction;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.utils.NodeUtil;

/**
 * This handler is associated with the EvaluateURI action.
 *
 * @author gbrown
 */
public class EvaluateURIActionHandler extends ProcessorActionHandler {

    private static final Logger log = Logger.getLogger(EvaluateURIActionHandler.class.getName());

    public static final String TEMPLATE_MUST_BE_SPECIFIED = "Template must be specified";

    private String pathTemplate;
    private List<String> queryParameters = new ArrayList<String>();

    /**
     * This constructor initialises the action.
     *
     * @param action The action
     */
    public EvaluateURIActionHandler(ProcessorAction action) {
        super(action);

        pathTemplate = ((EvaluateURIAction) getAction()).getTemplate();

        // If template contains a query string component, then separate the details
        if (pathTemplate != null && pathTemplate.indexOf('?') != -1) {
            int index = pathTemplate.indexOf('?');
            String queryString = pathTemplate.substring(index + 1);

            pathTemplate = pathTemplate.substring(0, index);

            StringTokenizer st = new StringTokenizer(queryString, "&");

            while (st.hasMoreTokens()) {
                String token = st.nextToken();

                if (token.charAt(0) == '{' && token.charAt(token.length() - 1) == '}') {
                    queryParameters.add(token.substring(1, token.length() - 1));
                } else {
                    // TODO: Needs reporting back to the configuration service (HWKBTM-230)
                    log.severe("Expecting query parameter template, e.g. {name}, but got '" + token + "'");
                }
            }
        }
    }

    /**
     * This method initialises the process action handler.
     *
     * @param processor The processor
     */
    @Override
    public List<ConfigMessage> init(Processor processor) {
        List<ConfigMessage> configMessages = super.init(processor);

        EvaluateURIAction action = (EvaluateURIAction) getAction();

        if (action.getTemplate() == null || action.getTemplate().trim().isEmpty()) {
            String message = "Template must be specified";
            log.severe(processor.getDescription() + ":" + getAction().getDescription() + ":" + message);
            ConfigMessage configMessage = new ConfigMessage();
            configMessage.setSeverity(Severity.Error);
            configMessage.setMessage(message);
            configMessage.setField("template");
            configMessage.setProcessor(processor.getDescription());
            configMessage.setAction(action.getDescription());
            configMessages.add(0, configMessage);
        }

        return configMessages;
    }

    @Override
    public boolean process(Trace trace, Node node, Direction direction, Map<String, ?> headers,
            Object[] values) {
        if (super.process(trace, node, direction, headers, values)) {
            if (node.getUri() != null && pathTemplate != null) {
                StringTokenizer uriTokens = new StringTokenizer(node.getUri(), "/");
                StringTokenizer templateTokens =
                        new StringTokenizer(pathTemplate, "/");

                if (uriTokens.countTokens() == templateTokens.countTokens()) {
                    Set<Property> props = null;
                    while (uriTokens.hasMoreTokens()) {
                        String uriToken = uriTokens.nextToken();
                        String templateToken = templateTokens.nextToken();

                        if (templateToken.charAt(0) == '{' && templateToken.charAt(templateToken.length() - 1) == '}') {
                            String name = templateToken.substring(1, templateToken.length() - 1);
                            if (props == null) {
                                props = new HashSet<Property>();
                            }
                            try {
                                props.add(new Property(name, URLDecoder.decode(uriToken, "UTF-8")));
                            } catch (UnsupportedEncodingException e) {
                                if (log.isLoggable(Level.FINEST)) {
                                    log.finest("Failed to decode value '" + uriToken + "': " + e);
                                }
                            }
                        } else if (!uriToken.equals(templateToken)) {
                            // URI template mismatch
                            return false;
                        }
                    }

                    // If properties extracted, then add to txn properties, and set the node's
                    // URI to the template, to make it stable/consistent - to make analytics easier
                    boolean processed = false;

                    if (props != null) {
                        node.getProperties().addAll(props);
                        NodeUtil.rewriteURI(node, pathTemplate);
                        processed = true;
                    }

                    // If query parameter template defined, then process
                    if (!queryParameters.isEmpty()) {
                        if (processQueryParameters(trace, node)) {
                            processed = true;
                        }
                    }

                    return processed;
                }
            }
        }

        return false;
    }

    /**
     * This method processes the query parameters associated with the supplied node to extract
     * templated named values as properties on the trace node.
     *
     * @param trace The trace
     * @param node The node
     * @return Whether query parameters were processed
     */
    protected boolean processQueryParameters(Trace trace, Node node) {
        boolean ret = false;

        // Translate query string into a map
        Set<Property> queryString = node.getProperties(Constants.PROP_HTTP_QUERY);
        if (!queryString.isEmpty()) {
            StringTokenizer st = new StringTokenizer(queryString.iterator().next().getValue(), "&");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                String[] namevalue = token.split("=");
                if (namevalue.length == 2) {
                    if (queryParameters.contains(namevalue[0])) {
                        try {
                            node.getProperties().add(new Property(namevalue[0],
                                    URLDecoder.decode(namevalue[1], "UTF-8")));
                            ret = true;
                        } catch (UnsupportedEncodingException e) {
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("Failed to decode value '" + namevalue[1] + "': " + e);
                            }
                        }
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest("Ignoring query parameter '" + namevalue[0] + "'");
                    }
                } else if (log.isLoggable(Level.FINEST)) {
                    log.finest("Query string part does not include name/value pair: " + token);
                }
            }
        }

        return ret;
    }
}
