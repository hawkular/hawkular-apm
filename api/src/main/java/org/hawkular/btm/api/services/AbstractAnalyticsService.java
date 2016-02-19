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
package org.hawkular.btm.api.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.hawkular.btm.api.model.analytics.PropertyInfo;
import org.hawkular.btm.api.model.analytics.URIInfo;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.model.btxn.Producer;
import org.hawkular.btm.api.model.config.btxn.BusinessTxnConfig;

/**
 * The abstract base class for implementations of the Analytics Service.
 *
 * @author gbrown
 */
public abstract class AbstractAnalyticsService implements AnalyticsService {

    private static final Logger log = Logger.getLogger(AbstractAnalyticsService.class.getName());

    @Inject
    private ConfigurationService configService;

    /**
     * This method gets the configuration service.
     *
     * @return The configuration service
     */
    public ConfigurationService getConfigurationService() {
        return this.configService;
    }

    /**
     * This method sets the configuration service.
     *
     * @param cs The configuration service
     */
    public void setConfigurationService(ConfigurationService cs) {
        this.configService = cs;
    }

    /**
     * This method returns the list of business transactions for the supplied criteria.
     *
     * @param tenantId The tenant
     * @param criteria The criteria
     * @return The list of fragments
     */
    protected abstract List<BusinessTransaction> getFragments(String tenantId, Criteria criteria);

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getUnboundURIs(java.lang.String,
     *                                  long, long, boolean)
     */
    @Override
    public List<URIInfo> getUnboundURIs(String tenantId, long startTime, long endTime, boolean compress) {
        Criteria criteria = new Criteria();
        criteria.setStartTime(startTime).setEndTime(endTime);

        List<BusinessTransaction> fragments = getFragments(tenantId, criteria);

        return (doGetUnboundURIs(tenantId, fragments, compress));
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getBoundURIs(java.lang.String, java.lang.String, long, long)
     */
    @Override
    public List<String> getBoundURIs(String tenantId, String businessTransaction, long startTime, long endTime) {
        List<String> ret = new ArrayList<String>();

        Criteria criteria = new Criteria();
        criteria.setBusinessTransaction(businessTransaction)
        .setStartTime(startTime)
        .setEndTime(endTime);

        List<BusinessTransaction> fragments = getFragments(tenantId, criteria);

        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);
            obtainURIs(btxn.getNodes(), ret);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AnalyticsService#getPropertyInfo(java.lang.String,
     *                      java.lang.String, long, long)
     */
    @Override
    public List<PropertyInfo> getPropertyInfo(String tenantId, String businessTransaction,
            long startTime, long endTime) {
        List<PropertyInfo> ret = new ArrayList<PropertyInfo>();
        List<String> propertyNames = new ArrayList<String>();

        Criteria criteria = new Criteria();
        criteria.setStartTime(startTime)
        .setEndTime(endTime)
        .setBusinessTransaction(businessTransaction);

        List<BusinessTransaction> fragments = getFragments(tenantId, criteria);

        // Process the fragments to identify which URIs are no used in any business transaction
        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);

            for (String property : btxn.getProperties().keySet()) {
                if (!propertyNames.contains(property)) {
                    propertyNames.add(property);
                    PropertyInfo pi = new PropertyInfo();
                    pi.setName(property);
                    ret.add(pi);
                }
            }
        }

        Collections.sort(ret, new Comparator<PropertyInfo>() {
            @Override
            public int compare(PropertyInfo arg0, PropertyInfo arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }
        });

        return ret;
    }

    // Don't collapse if a part node has a certain level of count, as may indicate a common component
    // Other indicate to collapse may be if multiple parts share the same child name? - but may only be
    // obvious after collapse - but could do a test collapse, and then check the result before making
    // permanent.
    // May need to experiment with different rules

    // Initial rule - collapse if a node contains more than a threshold of children

    /**
     * This method compresses the list of URIs to identify
     * common patterns.
     *
     * @param uris The URIs
     * @return The compressed list of URIs
     */
    protected static List<URIInfo> compressURIInfo(List<URIInfo> uris) {
        List<URIInfo> others = new ArrayList<URIInfo>();

        URIPart rootPart = new URIPart();

        for (int i = 0; i < uris.size(); i++) {
            URIInfo uri = uris.get(i);

            if (uri.getUri() != null
                    && uri.getUri().length() > 0
                    && uri.getUri().charAt(0) == '/') {
                String[] parts = uri.getUri().split("/");

                buildTree(rootPart, parts, 1, uri.getEndpointType());

            } else {
                others.add(new URIInfo(uri));
            }
        }

        // Construct new list
        List<URIInfo> info = null;

        if (uris.size() != others.size()) {
            rootPart.collapse();

            info = extractURIInfo(rootPart);

            info.addAll(others);
        } else {
            info = others;
        }

        // Initialise the URI info
        initURIInfo(info);

        return info;
    }

    /**
     * This method builds a tree.
     *
     * @param parent The current parent node
     * @param parts The parts of the URI being processed
     * @param index The current index into the parts array
     * @param endpointType The endpoint type
     */
    protected static void buildTree(URIPart parent, String[] parts, int index, String endpointType) {
        // Check if part is defined in the parent
        URIPart child = parent.addChild(parts[index]);

        if (index < parts.length - 1) {
            buildTree(child, parts, index + 1, endpointType);
        } else {
            child.setEndpointType(endpointType);
        }
    }

    /**
     * This method expands a tree into the collapsed set of URIs.
     *
     * @param root The tree
     * @return The list of URIs
     */
    protected static List<URIInfo> extractURIInfo(URIPart root) {
        List<URIInfo> uris = new ArrayList<URIInfo>();

        root.extractURIInfo(uris, "");

        return uris;
    }

    /**
     * This method initialises the list of URI information.
     *
     * @param uris The URI information
     */
    protected static void initURIInfo(List<URIInfo> uris) {
        for (int i = 0; i < uris.size(); i++) {
            URIInfo info = uris.get(i);

            info.setRegex(createRegex(info.getUri(), info.metaURI()));

            if (info.metaURI()) {
                StringBuilder template = new StringBuilder();

                String[] parts = info.getUri().split("/");

                String part = null;
                int paramNo = 1;

                for (int j = 1; j < parts.length; j++) {
                    template.append("/");

                    if (parts[j].equals("*")) {
                        if (part == null) {
                            template.append("{");
                            template.append("param");
                            template.append(paramNo++);
                            template.append("}");
                        } else {
                            // Check if plural
                            if (part.length() > 1 && part.charAt(part.length() - 1) == 's') {
                                part = part.substring(0, part.length() - 1);
                            }
                            template.append("{");
                            template.append(part);
                            template.append("Id}");
                        }
                        part = null;
                    } else {
                        part = parts[j];
                        template.append(part);
                    }
                }

                info.setTemplate(template.toString());
            }
        }
    }

    /**
     * This method obtains the unbound URIs from a list of business
     * transaction fragments.
     *
     * @param tenantId The tenant
     * @param fragments The list of business txn fragments
     * @param compress Whether the list should be compressed (i.e. to identify patterns)
     * @return The list of unbound URIs
     */
    protected List<URIInfo> doGetUnboundURIs(String tenantId,
            List<BusinessTransaction> fragments, boolean compress) {
        List<URIInfo> ret = new ArrayList<URIInfo>();
        Map<String, URIInfo> map = new HashMap<String, URIInfo>();

        // Process the fragments to identify which URIs are no used in any business transaction
        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);

            if (btxn.initialFragment() && !btxn.getNodes().isEmpty() && btxn.getName() == null) {

                // Check if top level node is Consumer
                if (btxn.getNodes().get(0) instanceof Consumer) {
                    Consumer consumer = (Consumer) btxn.getNodes().get(0);
                    String uri = consumer.getUri();

                    // Check whether URI already known, and that it did not result
                    // in a fault (e.g. want to ignore spurious URIs that are not
                    // associated with a valid transaction)
                    if (!map.containsKey(uri) && consumer.getFault() == null) {
                        URIInfo info = new URIInfo();
                        info.setUri(uri);
                        info.setEndpointType(consumer.getEndpointType());
                        ret.add(info);
                        map.put(uri, info);
                    }
                } else {
                    obtainProducerURIs(btxn.getNodes(), ret, map);
                }
            }
        }

        // Check whether any of the top level URIs are already associated with
        // a business txn config
        if (configService != null) {
            Map<String, BusinessTxnConfig> configs = configService.getBusinessTransactions(tenantId, 0);
            for (BusinessTxnConfig config : configs.values()) {
                if (config.getFilter() != null && config.getFilter().getInclusions() != null) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Remove unbound URIs associated with btxn config=" + config);
                    }
                    for (String filter : config.getFilter().getInclusions()) {

                        if (filter != null && filter.trim().length() > 0) {
                            Iterator<URIInfo> iter = ret.iterator();
                            while (iter.hasNext()) {
                                URIInfo info = iter.next();
                                if (Pattern.matches(filter, info.getUri())) {
                                    iter.remove();
                                }
                            }
                        }
                    }
                }
            }
        }

        // Check if the URIs should be compressed to identify common patterns
        if (compress) {
            ret = compressURIInfo(ret);
        }

        Collections.sort(ret, new Comparator<URIInfo>() {
            @Override
            public int compare(URIInfo arg0, URIInfo arg1) {
                return arg0.getUri().compareTo(arg1.getUri());
            }
        });

        return ret;
    }

    /**
     * This method collects the information regarding URIs.
     *
     * @param nodes The nodes
     * @param uris The list of URIs
     */
    protected void obtainURIs(List<Node> nodes, List<String> uris) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node.getUri() != null && !uris.contains(node.getUri())) {
                uris.add(node.getUri());
            }

            if (node instanceof ContainerNode) {
                obtainURIs(((ContainerNode) node).getNodes(), uris);
            }
        }
    }

    /**
     * This method collects the information regarding URIs for
     * contained producers.
     *
     * @param nodes The nodes
     * @param uris The list of URI info
     * @param map The map of URis to info
     */
    protected void obtainProducerURIs(List<Node> nodes, List<URIInfo> uris, Map<String, URIInfo> map) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node instanceof Producer) {
                String uri = node.getUri();

                if (!map.containsKey(uri)) {
                    URIInfo info = new URIInfo();
                    info.setUri(uri);
                    info.setEndpointType(((Producer) node).getEndpointType());
                    uris.add(info);
                    map.put(uri, info);
                }
            }

            if (node instanceof ContainerNode) {
                obtainProducerURIs(((ContainerNode) node).getNodes(), uris, map);
            }
        }
    }

    /**
     * This method derives the regular expression from the supplied
     * URI.
     *
     * @param uri The URI
     * @param meta Whether this is a meta URI
     * @return The regular expression
     */
    protected static String createRegex(String uri, boolean meta) {
        StringBuffer regex = new StringBuffer();

        regex.append('^');

        for (int i=0; i < uri.length(); i++) {
            char ch=uri.charAt(i);
            if ("*".indexOf(ch) != -1) {
                regex.append('.');
            } else if ("\\.^$|?+[]{}()".indexOf(ch) != -1) {
                regex.append('\\');
            }
            regex.append(ch);
        }

        regex.append('$');

        return regex.toString();
    }

    /**
     * This class represents a node in a tree of URI parts.
     *
     * @author gbrown
     */
    public static class URIPart {

        /**  */
        private static final int CHILD_THRESHOLD = 10;
        private int count = 1;
        private Map<String, URIPart> children;
        private String endpointType;

        /**
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * @param count the count to set
         */
        public void setCount(int count) {
            this.count = count;
        }

        /**
         * @return the children
         */
        public Map<String, URIPart> getChildren() {
            return children;
        }

        /**
         * @param children the children to set
         */
        public void setChildren(Map<String, URIPart> children) {
            this.children = children;
        }

        /**
         * This method adds a child with the supplied name,
         * if does not exist, or increments its count.
         *
         * @param name The name
         * @return The added/existing child
         */
        public URIPart addChild(String name) {
            URIPart child = null;

            if (children == null) {
                children = new HashMap<String, URIPart>();
            }

            if (!children.containsKey(name)) {
                child = new URIPart();
                children.put(name, child);
            } else {
                child = children.get(name);
                child.setCount(child.getCount() + 1);
            }

            return child;
        }

        /**
         * This method will apply rules to collapse the tree.
         */
        public void collapse() {
            if (children != null && !children.isEmpty()) {
                if (children.size() >= CHILD_THRESHOLD) {
                    URIPart merged = new URIPart();
                    for (URIPart cur : children.values()) {
                        merged.merge(cur);
                    }
                    children.clear();
                    children.put("*", merged);

                    merged.collapse();
                } else {
                    // Recursively perform on children
                    for (URIPart part : children.values()) {
                        part.collapse();
                    }
                }
            }
        }

        /**
         * This method merges the supplied URI part into
         * the current part.
         *
         * @param toMerge
         */
        public void merge(URIPart toMerge) {
            if (endpointType == null) {
                endpointType = toMerge.getEndpointType();
            }

            count += toMerge.getCount();

            // Process the supplied part's child nodes
            if (toMerge.getChildren() != null) {
                if (children == null) {
                    children = new HashMap<String, URIPart>();
                }
                for (String child : toMerge.getChildren().keySet()) {
                    if (getChildren().containsKey(child)) {
                        // Recursively merge
                        getChildren().get(child).merge(toMerge.getChildren().get(child));
                    } else {
                        // Move child to the merged tree
                        getChildren().put(child, toMerge.getChildren().get(child));
                    }
                }
            }
        }

        /**
         * This method expands the URIInfo from the tree.
         *
         * @param uris The list of URIs
         * @param uri The URI string
         */
        public void extractURIInfo(List<URIInfo> uris, String uri) {

            if (endpointType != null) {
                URIInfo info = new URIInfo();
                info.setUri(uri);
                info.setEndpointType(endpointType);
                uris.add(info);
            }

            if (getChildren() != null) {
                for (String child : getChildren().keySet()) {
                    URIPart part = getChildren().get(child);

                    part.extractURIInfo(uris, uri + "/" + child);
                }
            }
        }

        /**
         * @return the endpointType
         */
        public String getEndpointType() {
            return endpointType;
        }

        /**
         * @param endpointType the endpointType to set
         */
        public void setEndpointType(String endpointType) {
            this.endpointType = endpointType;
        }
    }
}
