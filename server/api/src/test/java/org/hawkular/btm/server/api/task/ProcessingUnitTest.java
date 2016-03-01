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
package org.hawkular.btm.server.api.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author gbrown
 */
public class ProcessingUnitTest {

    @Test
    public void testSingleResult() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>() {

            @Override
            public boolean isMultiple() {
                return false;
            }

            @Override
            public String processSingle(String item) throws Exception {
                return item;
            }

            @Override
            public List<String> processMultiple(String item) throws Exception {
                return null;
            }
        };

        pu.setProcessor(proc);

        List<String> results = new ArrayList<String>();

        pu.setResultHandler(new Handler<String>() {
            @Override
            public void handle(List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(source, results);
    }

    @Test
    public void testSingleRetry() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>() {

            @Override
            public boolean isMultiple() {
                return false;
            }

            @Override
            public String processSingle(String item) throws Exception {
                throw new Exception("PLEASE RETRY");
            }

            @Override
            public List<String> processMultiple(String item) throws Exception {
                return null;
            }
        };

        pu.setProcessor(proc);
        pu.setRetryCount(1);

        List<String> results = new ArrayList<String>();

        pu.setRetryHandler(new Handler<String>() {
            @Override
            public void handle(List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(source, results);
    }

    @Test
    public void testSingleRetryNoHandler() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>() {

            @Override
            public boolean isMultiple() {
                return false;
            }

            @Override
            public String processSingle(String item) throws Exception {
                if (item.equals("world")) {
                    throw new Exception("PLEASE RETRY");
                }
                return item;
            }

            @Override
            public List<String> processMultiple(String item) throws Exception {
                return null;
            }
        };

        pu.setProcessor(proc);

        List<String> results = new ArrayList<String>();

        pu.setResultHandler(new Handler<String>() {
            @Override
            public void handle(List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(1, results.size());
        assertEquals("hello", results.get(0));
    }

    @Test
    public void testMultipleResult() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>() {

            @Override
            public boolean isMultiple() {
                return true;
            }

            @Override
            public String processSingle(String item) throws Exception {
                return null;
            }

            @Override
            public List<String> processMultiple(String item) throws Exception {
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
            public void handle(List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(4, results.size());
    }

    @Test
    public void testMultipleRetry() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>() {

            @Override
            public boolean isMultiple() {
                return true;
            }

            @Override
            public String processSingle(String item) throws Exception {
                return null;
            }

            @Override
            public List<String> processMultiple(String item) throws Exception {
                throw new Exception("PLEASE RETRY");
            }
        };

        pu.setProcessor(proc);
        pu.setRetryCount(1);

        List<String> results = new ArrayList<String>();

        pu.setRetryHandler(new Handler<String>() {
            @Override
            public void handle(List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(source, results);
    }

    @Test
    public void testMultipleRetryNoHandler() {
        ProcessingUnit<String, String> pu = new ProcessingUnit<String, String>();

        Processor<String, String> proc = new AbstractProcessor<String, String>() {

            @Override
            public boolean isMultiple() {
                return true;
            }

            @Override
            public String processSingle(String item) throws Exception {
                return null;
            }

            @Override
            public List<String> processMultiple(String item) throws Exception {
                if (item.equals("world")) {
                    throw new Exception("PLEASE RETRY");
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
            public void handle(List<String> items) throws Exception {
                results.addAll(items);
            }
        });

        List<String> source = new ArrayList<String>();
        source.add("hello");
        source.add("world");

        try {
            pu.handle(source);
        } catch (Exception e) {
            fail("Failed to process: " + e);
        }

        assertEquals(2, results.size());
        assertEquals("hello", results.get(0));
        assertEquals("hello", results.get(1));
    }

}
