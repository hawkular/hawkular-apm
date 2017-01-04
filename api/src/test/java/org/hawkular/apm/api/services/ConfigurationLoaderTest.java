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
package org.hawkular.apm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Paths;

import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.junit.After;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ConfigurationLoaderTest {

    @After
    public void endTest() {
        System.setProperty("HAWKULAR_APM_CONFIG", "");
    }

    @Test
    public void testLoadConfigFromClasspath() {
        System.setProperty("HAWKULAR_APM_CONFIG", "cpconfig");

        CollectorConfiguration cc = ConfigurationLoader.getConfiguration(null);

        assertNotNull(cc);
        assertNotNull(cc.getTransactions());
        assertNotNull(cc.getTransactions().get("cptest"));

        assertEquals("Classpath test", cc.getTransactions().get("cptest").getDescription());
    }

    @Test
    public void testLoadConfigFromRelativePath() {
        System.setProperty("HAWKULAR_APM_CONFIG", "src/relconfig");

        CollectorConfiguration cc = ConfigurationLoader.getConfiguration(null);

        assertNotNull(cc);

        assertNotNull(cc);
        assertNotNull(cc.getTransactions());
        assertNotNull(cc.getTransactions().get("reltest"));

        assertEquals("Relative path test", cc.getTransactions().get("reltest").getDescription());
    }

    @Test
    public void testLoadConfigFromAbsolutePath() {
        String path=Paths.get("").toFile().getAbsolutePath()+
                java.io.File.separator+"src"+java.io.File.separator+"absconfig";
        System.setProperty("HAWKULAR_APM_CONFIG", path);

        CollectorConfiguration cc = ConfigurationLoader.getConfiguration(null);

        assertNotNull(cc);

        assertNotNull(cc);
        assertNotNull(cc.getTransactions());
        assertNotNull(cc.getTransactions().get("abstest"));

        assertEquals("Absolute path test", cc.getTransactions().get("abstest").getDescription());
    }

}
