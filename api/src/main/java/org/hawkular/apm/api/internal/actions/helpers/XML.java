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
package org.hawkular.apm.api.internal.actions.helpers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * @author gbrown
 */
public class XML {

    private static final Logger log = Logger.getLogger(XML.class.getName());

    private static final String DEFAULT_INDENT = "yes";
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * This method serializes the supplied XML object to a string.
     *
     * @param node The node
     * @return The string, or null if an error occurred
     */
    public static String serialize(Object node) {
        String ret = null;

        if (node instanceof String) {
            ret = (String) node;
        } else if (node instanceof byte[]) {
            ret = new String((byte[]) node);
        } else if (node instanceof DOMSource) {
            ret = serializeDOMSource((DOMSource) node);
        } else if (node instanceof Node) {
            ret = serializeNode((Node) node);
        } else {
            log.severe("Unable to serialize '" + node + "'");
        }

        if (ret != null) {
            ret = ret.trim();
        }

        return ret;
    }

    /**
     * This method evaluates the predicate based on the xpath
     * expression on the supplied node.
     *
     * @param xpath The xpath expression
     * @param node The node
     * @return The result
     */
    public static boolean predicate(String xpath, Object node) {
        Node domNode = getNode(node);

        if (domNode == null) {
            log.severe("Unable to evaluate non DOM Node object");
            return false;
        }

        if (xpath == null) {
            log.severe("Predicate has not xpath expression");
            return false;
        }

        // TODO: HWKBTM-104 Investigate caching compiled xpath expressions
        try {
            xpath = getExpression(xpath);
            XPath xp = XPathFactory.newInstance().newXPath();
            Boolean result = (Boolean) xp.evaluate(xpath, domNode, XPathConstants.BOOLEAN);

            if (result != null) {
                return result;
            }
        } catch (XPathExpressionException e) {
            log.log(Level.SEVERE, "Failed to execute predicate xpath '" + xpath + "'", e);
        }

        return false;
    }

    /**
     * This method evaluates the xpath expression on the supplied
     * node. The result can either be a document fragment, a
     * text node value or null if the expression references a
     * missing part of the document.
     *
     * @param xpath The xpath expression
     * @param node The node
     * @return The result, or null if not found (which may be due to an expression error)
     */
    public static String evaluate(String xpath, Object node) {
        Node domNode = getNode(node);

        if (domNode == null) {
            log.severe("Unable to evaluate non DOM Node object");
            return null;
        }

        // If no xpath expression, then serialize
        if (xpath == null || xpath.trim().isEmpty()) {
            return serialize(node);
        }

        // TODO: HWKBTM-104 Investigate caching compiled xpath expressions
        try {
            xpath = getExpression(xpath);
            XPath xp = XPathFactory.newInstance().newXPath();
            Node result = (Node) xp.evaluate(xpath, domNode, XPathConstants.NODE);

            if (result != null) {
                if (result.getNodeType() == Node.TEXT_NODE) {
                    return result.getNodeValue();
                } else if (result.getNodeType() == Node.ATTRIBUTE_NODE) {
                    return result.getNodeValue();
                }
                return serialize(result);
            }
        } catch (DOMException|XPathExpressionException e) {
            log.log(Level.SEVERE, "Failed to evaluate xpath '" + xpath + "'", e);
        }

        return null;
    }

    /**
     * This method transforms the XPath expression to replace XPath 2.0
     * namespace wildcards with use of the local-name() function.
     *
     * @param expr
     * @return
     */
    protected static String getExpression(String expr) {
        StringBuilder buf=new StringBuilder(expr);
        int startIndex=-1;
        do {
            startIndex=buf.indexOf("*:");
            if (startIndex != -1) {
                int endIndex = buf.indexOf("/", startIndex+2);
                if (endIndex == -1) {
                    endIndex = buf.length();
                }
                buf.replace(startIndex, endIndex, "*[local-name()='"+buf.substring(startIndex+2, endIndex)+"']");
            }
        } while (startIndex != -1);

        return buf.toString();
    }

    /**
     * This method serializes the supplied DOM node to a string.
     *
     * @param node The node
     * @return The string, or null if an error occurred
     */
    protected static String serializeNode(Node node) {
        return serializeDOMSource(new DOMSource(node));
    }

    /**
     * This method serializes the supplied DOM node to a string.
     *
     * @param domSource The DOM source
     * @return The string, or null if an error occurred
     */
    protected static String serializeDOMSource(DOMSource domSource) {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, DEFAULT_ENCODING);
            transformer.setOutputProperty(OutputKeys.INDENT, DEFAULT_INDENT);
            transformer.transform(domSource, result);
            writer.flush();
            return writer.toString();
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Failed to serialize node", e);
        }
        return null;
    }

    /**
     * This method serializes the supplied XML object to a string.
     *
     * @param node The node
     * @return The string, or null if an error occurred
     */
    protected static Node deserialize(Object node) {
        if (node instanceof Node) {
            return (Node) node;
        } else if (node instanceof byte[]) {
            return deserializeString(new String((byte[]) node));
        } else if (node instanceof String) {
            return deserializeString((String) node);
        } else {
            log.severe("Unable to serialize '" + node + "'");
        }
        return null;
    }

    /**
     * This method deserializes the supplied document.
     *
     * @param doc The XML document
     * @return The DOM node, or null if an error occurred
     */
    protected static Node deserializeString(String doc) {
        try {
            StringReader reader = new StringReader(doc);
            StreamSource source = new StreamSource(reader);
            DOMResult result = new DOMResult();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, DEFAULT_ENCODING);
            transformer.setOutputProperty(OutputKeys.INDENT, DEFAULT_INDENT);
            transformer.transform(source, result);
            return result.getNode();
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Failed to serialize node", e);
        }
        return null;
    }

    /**
     * This method obtains the node, identified by the xpath
     * expression, from the supplied node.
     *
     * @param xpath The xpath expression
     * @param node The root node
     * @return The selected node, or null if not found
     */
    protected static Node selectNode(String xpath, Object node) {
        Node domNode = getNode(node);

        if (domNode == null) {
            log.severe("Unable to select node for non DOM Node object");
            return null;
        }

        // TODO: HWKBTM-104 Investigate caching compiled xpath expressions
        try {
            xpath = getExpression(xpath);
            XPath xp = XPathFactory.newInstance().newXPath();
            return (Node) xp.evaluate(xpath, domNode, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            log.log(Level.SEVERE, "Failed to select node for xpath '" + xpath + "'", e);
        }

        return null;
    }

    /**
     * This method converts the supplied object to a DOM
     * node.
     *
     * @param node The object
     * @return The node, or null if cannot convert
     */
    protected static Node getNode(Object node) {
        if (node instanceof Node) {
            return (Node) node;
        } else if (node instanceof DOMSource) {
            return ((DOMSource) node).getNode();
        } else {
            Node n = deserialize(node);

            if (n == null) {
                log.severe("Cannot convert '" + node + "' to DOM node");
            } else {
                return n;
            }
        }
        return null;
    }
}
