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
package org.hawkular.apm.client.collector.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.model.config.Direction;
import org.hawkular.apm.api.model.config.txn.AddContentAction;
import org.hawkular.apm.api.model.config.txn.AddCorrelationIdAction;
import org.hawkular.apm.api.model.config.txn.DataSource;
import org.hawkular.apm.api.model.config.txn.LiteralExpression;
import org.hawkular.apm.api.model.config.txn.Processor;
import org.hawkular.apm.api.model.config.txn.SetPropertyAction;
import org.hawkular.apm.api.model.config.txn.TextExpression;
import org.hawkular.apm.api.model.config.txn.TransactionConfig;
import org.hawkular.apm.api.model.trace.Component;
import org.hawkular.apm.api.model.trace.Consumer;
import org.hawkular.apm.api.model.trace.Content;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier;
import org.hawkular.apm.api.model.trace.CorrelationIdentifier.Scope;
import org.hawkular.apm.api.model.trace.Message;
import org.hawkular.apm.api.model.trace.NodeType;
import org.hawkular.apm.api.model.trace.Trace;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ProcessorManagerTest {

    @Test
    public void testNodeTypeInNoURIFilter() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);
        p1.getActions().add(pa1);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals("second", service.getProperties("test").iterator().next().getValue());
    }

    @Test
    public void testNodeTypeInNoURIFilterWithPredicate() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        LiteralExpression literal = new LiteralExpression();
        literal.setValue("true");
        pa1.setPredicate(literal);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals("second", service.getProperties("test").iterator().next().getValue());
    }

    @Test
    public void testNodeTypeInNoURIFilterWithPredicateNoAction() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        LiteralExpression literal = new LiteralExpression();
        literal.setValue("false");
        pa1.setPredicate(literal);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testNodeTypeOutNoURIFilterNoAction() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.Out, null, "first", "second");

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testMismatchedNodeTypeInNoURIFilterNoAction() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testNodeTypeInWithURIFilter() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setUriFilter("include");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        service.setUri("should include this");
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals("second", service.getProperties("test").iterator().next().getValue());
    }

    @Test
    public void testNodeTypeInWithURIFilterNoAction() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setUriFilter("include");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        service.setUri("should exclude this");
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testNodeTypeInNoURIFilterSetProperty() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals("second", service.getProperties("test").iterator().next().getValue());
    }

    @Test
    public void testNodeTypeInNoURIFilterSetFault() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName(Constants.PROP_FAULT);
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(1, service.getProperties(Constants.PROP_FAULT).size());
        assertEquals("second", service.getProperties(Constants.PROP_FAULT).iterator().next().getValue());
    }

    @Test
    public void testNodeTypeInNoURIFilterAddContent() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);

        AddContentAction pa1 = new AddContentAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        pa1.setType("MessageType");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(1, service.getIn().getContent().size());

        Content content = service.getIn().getContent().get("test");

        assertNotNull(content);

        assertEquals("MessageType", content.getType());
        assertEquals("second", content.getValue());
    }

    @Test
    public void testNodeTypeInAddCorrelationId() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);

        AddCorrelationIdAction pa1 = new AddCorrelationIdAction();
        p1.getActions().add(pa1);

        pa1.setScope(Scope.ControlFlow);
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(1, service.getCorrelationIds().size());

        assertEquals(CorrelationIdentifier.Scope.ControlFlow, service.getCorrelationIds().get(0).getScope());
        assertEquals("second", service.getCorrelationIds().get(0).getValue());
    }

    @Test
    public void testNodeTypeInFaultFilterNoFault() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);
        p1.setFaultFilter("MyFault");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");

        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(0, trace.allProperties().size());
    }

    @Test
    public void testNodeTypeInFaultFilterWithMismatchFault() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);
        p1.setFaultFilter("MyFault");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        service.getProperties().add(new Property(Constants.PROP_FAULT, "NotSameFault"));

        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        pm.process(trace, service, Direction.In, null, "first", "second");

        // The 'fault' property
        assertEquals(1, trace.allProperties().size());
    }

    @Test
    public void testNodeTypeInFaultFilterWithMatchingFault() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);
        p1.setFaultFilter("MyFault");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        service.getProperties().add(new Property(Constants.PROP_FAULT, "MyFault"));

        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        pm.process(trace, service, Direction.In, null, "first", "second");

        // Including the 'fault' property
        assertEquals(2, trace.allProperties().size());
        assertTrue(trace.hasProperty("result"));
    }

    @Test
    public void testNodeTypeInNoOperation() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setOperation("MyOp");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("OperationFound");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(0, trace.allProperties().size());
    }

    @Test
    public void testNodeTypeInWithMismatchOperation() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setOperation("MyOp");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        service.setOperation("NotSameOperation");

        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(0, trace.allProperties().size());
    }

    @Test
    public void testNodeTypeInWithMatchingFault() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setOperation("MyOp");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        service.setOperation("MyOp");

        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals(1, trace.allProperties().size());
        assertTrue(trace.hasProperty("result"));
    }

    @Test
    public void testIsProcessedTrue() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setOperation("MyOp");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        service.setOperation("MyOp");

        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        assertTrue(pm.isProcessed(trace, service, Direction.In));
    }

    @Test
    public void testIsProcessedFalse() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);
        p1.setUriFilter("include");

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        service.setUri("should exclude this");
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        assertFalse(pm.isProcessed(trace, service, Direction.In));
    }

    @Test
    public void testIsContentProcessedFalse() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("result");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("FaultRecorded");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();

        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        assertFalse(pm.isContentProcessed(trace, service, Direction.In));
    }

    @Test
    public void testIsContentProcessedTrueForAddContent() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);

        AddContentAction pa1 = new AddContentAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        pa1.setType("MessageType");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        assertTrue(pm.isContentProcessed(trace, service, Direction.In));
    }

    @Test
    public void testIsContentProcessedTrueForSetProperty() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Consumer);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Consumer service = new Consumer();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Message req = new Message();
        service.setIn(req);

        assertTrue(pm.isContentProcessed(trace, service, Direction.In));
    }

    @Test
    public void testProcessInNoHeadersOrContent() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        LiteralExpression literal = new LiteralExpression();
        literal.setValue("hello");
        pa1.setExpression(literal);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null);

        assertEquals("hello", service.getProperties("test").iterator().next().getValue());
    }

    @Test
    public void testProcessInNoHeaders() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");

        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Header);
        expr.setKey("hello");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null);

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testProcessInWithHeaders() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Header);
        expr.setKey("hello");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("hello", "world");
        pm.process(trace, service, Direction.In, headers);

        assertTrue(service.hasProperty("test"));
    }

    @Test
    public void testProcessInNoContent() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("0");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null);

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testProcessInWithContent() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("0");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "hello");

        assertTrue(service.hasProperty("test"));
    }

    @Test
    public void testProcessorPredicateTrue() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        LiteralExpression literal = new LiteralExpression();
        literal.setValue("true");
        p1.setPredicate(literal);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertEquals("second", service.getProperties("test").iterator().next().getValue());
    }

    @Test
    public void testProcessorPredicateFalse() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        p1.setNodeType(NodeType.Component);
        p1.setDirection(Direction.In);

        LiteralExpression literal = new LiteralExpression();
        literal.setValue("false");
        p1.setPredicate(literal);

        SetPropertyAction pa1 = new SetPropertyAction();
        p1.getActions().add(pa1);

        pa1.setName("test");
        TextExpression expr = new TextExpression();
        expr.setSource(DataSource.Content);
        expr.setKey("1");
        pa1.setExpression(expr);

        ProcessorManager pm = new ProcessorManager(cc);

        Trace trace = new Trace();
        Component service = new Component();
        trace.getNodes().add(service);
        trace.setTransaction("testapp");

        pm.process(trace, service, Direction.In, null, "first", "second");

        assertFalse(service.hasProperty("test"));
    }

    @Test
    public void testInit() {
        CollectorConfiguration cc = new CollectorConfiguration();

        TransactionConfig btc = new TransactionConfig();
        cc.getTransactions().put("testapp", btc);

        Processor p1 = new Processor();
        btc.getProcessors().add(p1);

        ProcessorManager pm = new ProcessorManager(cc);

        assertTrue(pm.getProcessors().containsKey("testapp"));
        assertEquals(1, pm.getProcessors().get("testapp").size());
        assertEquals(p1, pm.getProcessors().get("testapp").get(0).getProcessor());

        // Update the configuration
        TransactionConfig btc2 = new TransactionConfig();

        Processor p2 = new Processor();
        btc2.getProcessors().add(p2);

        pm.init("testapp", btc2);

        // Check that the changed processor has been initialised
        assertTrue(pm.getProcessors().containsKey("testapp"));
        assertEquals(1, pm.getProcessors().get("testapp").size());
        assertEquals(p2, pm.getProcessors().get("testapp").get(0).getProcessor());
    }

}
