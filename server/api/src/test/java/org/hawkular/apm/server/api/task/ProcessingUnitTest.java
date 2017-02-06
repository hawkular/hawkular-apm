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
package org.hawkular.apm.server.api.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.server.api.task.Processor.ProcessorType;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ProcessingUnitTest {

    @Test
    public void testSingleResult() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>(ProcessorType.OneToOne) {

            @Override
            public String processOneToOne(String tenantId, String item) throws RetryAttemptException {
                return item;
            }
        };

        pu.setProcessor(proc);

        List<String> results = new ArrayList<String>();

        pu.setResultHandler(new Handler<String>() {
            @Override
            public void handle(String tenantId, List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(null, source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(source, results);
    }

    @Test
    public void testSingleRetry() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>(ProcessorType.OneToOne) {

            @Override
            public String processOneToOne(String tenantId, String item) throws RetryAttemptException {
                throw new RetryAttemptException("PLEASE RETRY");
            }
        };

        pu.setProcessor(proc);
        pu.setRetryCount(1);

        List<String> results = new ArrayList<String>();

        pu.setRetryHandler(new Handler<String>() {
            @Override
            public void handle(String tenantId, List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(null, source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(source, results);
    }

    @Test
    public void testSingleRetryNoHandler() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>(ProcessorType.OneToOne) {

            @Override
            public String processOneToOne(String tenantId, String item) throws RetryAttemptException {
                if (item.equals("world")) {
                    throw new RetryAttemptException("PLEASE RETRY");
                }
                return item;
            }
        };

        pu.setProcessor(proc);

        List<String> results = new ArrayList<String>();

        pu.setResultHandler(new Handler<String>() {
            @Override
            public void handle(String tenantId, List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(null, source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(1, results.size());
        assertEquals("hello", results.get(0));
    }

    @Test
    public void testMultipleResult() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>(ProcessorType.OneToMany) {

            @Override
            public List<String> processOneToMany(String tenantId, String item) throws RetryAttemptException {
                List<String> ret = new ArrayList<String>();
                ret.add(item);
                ret.add(item);
                return ret;
            }
        };

        pu.setProcessor(proc);

        List<String> results = new ArrayList<String>();

        pu.setResultHandler(new Handler<String>() {
            @Override
            public void handle(String tenantId, List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(null, source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(4, results.size());
    }

    @Test
    public void testMultipleRetry() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>(ProcessorType.OneToMany) {

            @Override
            public List<String> processOneToMany(String tenantId, String item) throws RetryAttemptException {
                throw new RetryAttemptException("PLEASE RETRY");
            }
        };

        pu.setProcessor(proc);
        pu.setRetryCount(1);

        List<String> results = new ArrayList<String>();

        pu.setRetryHandler(new Handler<String>() {
            @Override
            public void handle(String tenantId, List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(null, source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(source, results);
    }

    @Test
    public void testMultipleRetryNoHandler() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>(ProcessorType.OneToMany) {

            @Override
            public List<String> processOneToMany(String tenantId, String item) throws RetryAttemptException {
                if (item.equals("world")) {
                    throw new RetryAttemptException("PLEASE RETRY");
                }
                List<String> ret = new ArrayList<String>();
                ret.add(item);
                ret.add(item);
                return ret;
            }
        };

        pu.setProcessor(proc);

        List<String> results = new ArrayList<String>();

        pu.setResultHandler(new Handler<String>() {
            @Override
            public void handle(String tenantId, List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(null, source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(2, results.size());
        assertEquals("hello", results.get(0));
        assertEquals("hello", results.get(1));
    }

}
