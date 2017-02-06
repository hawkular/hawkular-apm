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
package org.hawkular.apm.api.model.config.instrumentation.jvm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author gbrown
 */
public class JVMInstrumentRuleTest {

    @Test
    public void testIsVersionValidNoVersions() {
        JVM rule = new JVM();

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidNoVersionsNull() {
        JVM rule = new JVM();

        assertTrue(rule.isVersionValid(null));
    }

    @Test
    public void testIsVersionValidFromVersionBefore() {
        JVM rule = new JVM();
        rule.setFromVersion("2.0.0");

        assertFalse(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidFromVersionSame() {
        JVM rule = new JVM();
        rule.setFromVersion("1.2.3");

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidFromVersionAfter() {
        JVM rule = new JVM();
        rule.setFromVersion("1.0.0");

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidFromVersionNull() {
        JVM rule = new JVM();
        rule.setFromVersion("1.0.0");

        assertTrue(rule.isVersionValid(null));
    }

    @Test
    public void testIsVersionValidToVersionBefore() {
        JVM rule = new JVM();
        rule.setToVersion("2.0.0");

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidToVersionSame() {
        JVM rule = new JVM();
        rule.setToVersion("1.2.3");

        assertFalse(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidToVersionAfter() {
        JVM rule = new JVM();
        rule.setToVersion("1.0.0");

        assertFalse(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidToVersionNull() {
        JVM rule = new JVM();
        rule.setToVersion("1.0.0");

        assertFalse(rule.isVersionValid(null));
    }
}
