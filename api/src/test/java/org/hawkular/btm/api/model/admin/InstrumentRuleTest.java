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
package org.hawkular.btm.api.model.admin;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumentRuleTest {

    @Test
    public void testIsVersionValidNoVersions() {
        InstrumentRule rule=new InstrumentRule();

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidNoVersionsNull() {
        InstrumentRule rule=new InstrumentRule();

        assertTrue(rule.isVersionValid(null));
    }

    @Test
    public void testIsVersionValidFromVersionBefore() {
        InstrumentRule rule=new InstrumentRule();
        rule.setFromVersion("2.0.0");

        assertFalse(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidFromVersionSame() {
        InstrumentRule rule=new InstrumentRule();
        rule.setFromVersion("1.2.3");

        assertTrue(rule.isVersionValid("1.2.3"));
    }


    @Test
    public void testIsVersionValidFromVersionAfter() {
        InstrumentRule rule=new InstrumentRule();
        rule.setFromVersion("1.0.0");

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidFromVersionNull() {
        InstrumentRule rule=new InstrumentRule();
        rule.setFromVersion("1.0.0");

        assertTrue(rule.isVersionValid(null));
    }

    @Test
    public void testIsVersionValidToVersionBefore() {
        InstrumentRule rule=new InstrumentRule();
        rule.setToVersion("2.0.0");

        assertTrue(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidToVersionSame() {
        InstrumentRule rule=new InstrumentRule();
        rule.setToVersion("1.2.3");

        assertFalse(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidToVersionAfter() {
        InstrumentRule rule=new InstrumentRule();
        rule.setToVersion("1.0.0");

        assertFalse(rule.isVersionValid("1.2.3"));
    }

    @Test
    public void testIsVersionValidToVersionNull() {
        InstrumentRule rule=new InstrumentRule();
        rule.setToVersion("1.0.0");

        assertFalse(rule.isVersionValid(null));
    }
}
