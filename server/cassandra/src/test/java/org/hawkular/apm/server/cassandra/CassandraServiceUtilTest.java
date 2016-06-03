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
package org.hawkular.apm.server.cassandra;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author gbrown
 */
public class CassandraServiceUtilTest {

    @Test
    public void testGetPosition() {
        long curTime = System.currentTimeMillis();
        long interval = 60000;

        long startTime = curTime - 150000;

        assertEquals(2, CassandraServiceUtil.getPosition(startTime, interval, curTime));
    }

    @Test
    public void testGetBaseTimestamp() {
        long interval = 60000;

        long startTime = 90000;

        int index = 1;

        assertEquals(120000, CassandraServiceUtil.getBaseTimestamp(startTime, interval, index));
    }

}
