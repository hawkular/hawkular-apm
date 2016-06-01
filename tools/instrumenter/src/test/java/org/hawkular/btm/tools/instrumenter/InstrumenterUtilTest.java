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
package org.hawkular.btm.tools.instrumenter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.junit.Test;

/**
 * @author gbrown
 */
public class InstrumenterUtilTest {

    @Test
    public void testGetCollectorConfiguration() {
        String version = System.getProperty("hawkular-btm-version");

        if (version == null) {
            fail("Version not defined with property 'hawkular-btm-version'");
        }

        try {
            CollectorConfiguration config = InstrumenterUtil.getCollectorConfiguration("jvm", version, true);

            assertNotNull(config);

        } catch (Exception e) {
            fail("Failed to get config: " + e);
        }
    }

}
