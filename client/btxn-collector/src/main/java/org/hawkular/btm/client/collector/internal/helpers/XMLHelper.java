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
package org.hawkular.btm.client.collector.internal.helpers;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.hawkular.btm.api.logging.Logger;
import org.hawkular.btm.api.logging.Logger.Level;
import org.w3c.dom.Node;

/**
 * @author gbrown
 */
public class XMLHelper {

    private static final Logger log = Logger.getLogger(XMLHelper.class.getName());

    /**  */
    private static final String DEFAULT_INDENT = "yes";
    /**  */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * This method serializes the supplied XML object to a string.
     *
     * @param node The node
     * @return The string, or null if an error occurred
     */
    public static String serialize(Object node) {
        if (node instanceof String) {
            return (String)node;
        } else if (node instanceof DOMSource) {
            return serializeDOMSource((DOMSource)node);
        } else if (node instanceof Node) {
            return serializeNode((Node)node);
        } else {
            log.severe("Unable to serialize '"+node+"'");
        }
        return null;
    }

    /**
     * This method serializes the supplied DOM node to a string.
     *
     * @param node The node
     * @return The string, or null if an error occurred
     */
    public static String serializeNode(Node node) {
        return serializeDOMSource(new DOMSource(node));
    }

    /**
     * This method serializes the supplied DOM node to a string.
     *
     * @param domSource The DOM source
     * @return The string, or null if an error occurred
     */
    public static String serializeDOMSource(DOMSource domSource) {
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
     * This method evaluates the xpath expression on the supplied
     * node.
     *
     * @param xpath The xpath expression
     * @param node The node
     * @return The result, or null if error occurred
     */
    public static String evaluate(String xpath, Object node) {
        Node domNode = getNode(node);

        if (domNode == null) {
            log.severe("Unable to evaluate non DOM Node object");
            return null;
        }

        // TODO: HWKBTM-104 Investigate caching compiled xpath expressions
        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            return xp.evaluate(xpath, domNode);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to evaluate xpath '"+xpath+"'", e);
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
            return (Node)node;
        } else if (node instanceof DOMSource) {
            return ((DOMSource)node).getNode();
        } else {
            log.severe("Cannot convert '"+node+"' to DOM node");
        }
        return null;
    }
}
