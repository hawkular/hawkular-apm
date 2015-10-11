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
package org.hawkular.btm.api.model.config.btxn;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author gbrown
 */
public class XMLTest {

    @Test
    public void testPredicateValue() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Value);
        xml.setKey("0");
        xml.setXpath("myxpath");

        assertEquals("XML.predicate(\"myxpath\",values[0])", xml.predicateText());
    }

    @Test
    public void testPredicateValueNoXpath() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Value);
        xml.setKey("0");

        assertEquals("XML.predicate(null,values[0])", xml.predicateText());
    }

    @Test
    public void testEvaluateValue() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Value);
        xml.setKey("0");
        xml.setXpath("myxpath");

        assertEquals("XML.evaluate(\"myxpath\",values[0])", xml.evaluateText());
    }

    @Test
    public void testEvaluateValueNoXpath() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Value);
        xml.setKey("0");

        assertEquals("XML.evaluate(null,values[0])", xml.evaluateText());
    }

    @Test
    public void testEvaluateValueEmptyXpath() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Value);
        xml.setKey("0");
        xml.setXpath(" ");

        assertEquals("XML.evaluate(null,values[0])", xml.evaluateText());
    }

    @Test
    public void testPredicateHeader() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Header);
        xml.setKey("mykey");
        xml.setXpath("myxpath");

        assertEquals("XML.predicate(\"myxpath\",headers.get(\"mykey\"))", xml.predicateText());
    }

    @Test
    public void testEvaluateHeader() {
        XMLExpression xml = new XMLExpression();
        xml.setSource(DataSource.Header);
        xml.setKey("mykey");
        xml.setXpath("myxpath");

        assertEquals("XML.evaluate(\"myxpath\",headers.get(\"mykey\"))", xml.evaluateText());
    }

}
