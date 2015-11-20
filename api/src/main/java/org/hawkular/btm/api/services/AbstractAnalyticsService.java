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
package org.hawkular.btm.api.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.btm.api.model.analytics.URIInfo;

/**
 * The abstract base class for implementations of the Analytics Service.
 *
 * @author gbrown
 */
public abstract class AbstractAnalyticsService implements AnalyticsService {

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
                others.add(uri);
            }
        }

        if (others.size() == uris.size()) {
            return uris;
        }

        // Construct new list
        rootPart.collapse();

        List<URIInfo> info = extractURIInfo(rootPart);

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
                            if (part.length() > 1 && part.charAt(part.length()-1) == 's') {
                                part = part.substring(0, part.length()-1);
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
     * This method derives the regular expression from the supplied
     * URI.
     *
     * @param uri The URI
     * @param meta Whether this is a meta URI
     * @return The regular expression
     */
    protected static String createRegex(String uri, boolean meta) {
        String regex = "^" + uri.replaceAll("/", "\\\\/") + "$";

        if (meta) {
            regex = regex.replaceAll("\\*", ".*");
        }

        return regex;
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
