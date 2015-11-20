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
package org.hawkular.btm.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hawkular.btm.api.model.btxn.Consumer;
import org.junit.Test;

/**
 * @author gbrown
 */
public class NodeUtilTest {

    /**  */
    private static final String NEW_URI = "/new/uri";
    /**  */
    private static final String ORIGINAL_URI = "/original/uri";

    @Test
    public void testRewriteURI() {
        Consumer consumer = new Consumer();
        consumer.setUri(ORIGINAL_URI);

        NodeUtil.rewriteURI(consumer, NEW_URI);

        assertEquals(NEW_URI, consumer.getUri());
    }

    @Test
    public void testIsOriginalURINotRewritten() {
        Consumer consumer = new Consumer();
        consumer.setUri(ORIGINAL_URI);

        assertTrue(NodeUtil.isOriginalURI(consumer, ORIGINAL_URI));
    }

    @Test
    public void testIsOriginalURIRewritten() {
        Consumer consumer = new Consumer();
        consumer.setUri(ORIGINAL_URI);

        NodeUtil.rewriteURI(consumer, NEW_URI);

        assertTrue(NodeUtil.isOriginalURI(consumer, ORIGINAL_URI));
    }

}
