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
package org.hawkular.btm.api.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.file.Paths;

import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.junit.After;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ConfigurationLoaderTest {

    @After
    public void endTest() {
        System.setProperty("hawkular-btm.config", "");
    }

    @Test
    public void testLoadConfigFromClasspath() {
        System.setProperty("hawkular-btm.config", "cpconfig");

        CollectorConfiguration cc = ConfigurationLoader.getConfiguration(null);

        assertNotNull(cc);
        assertNotNull(cc.getBusinessTransactions());
        assertNotNull(cc.getBusinessTransactions().get("cptest"));

        assertEquals("Classpath test", cc.getBusinessTransactions().get("cptest").getDescription());
    }

    @Test
    public void testLoadConfigFromRelativePath() {
        System.setProperty("hawkular-btm.config", "src/relconfig");

        CollectorConfiguration cc = ConfigurationLoader.getConfiguration(null);

        assertNotNull(cc);

        assertNotNull(cc);
        assertNotNull(cc.getBusinessTransactions());
        assertNotNull(cc.getBusinessTransactions().get("reltest"));

        assertEquals("Relative path test", cc.getBusinessTransactions().get("reltest").getDescription());
    }

    @Test
    public void testLoadConfigFromAbsolutePath() {
        String path=Paths.get("").toFile().getAbsolutePath()+
                java.io.File.separator+"src"+java.io.File.separator+"absconfig";
        System.setProperty("hawkular-btm.config", path);

        CollectorConfiguration cc = ConfigurationLoader.getConfiguration(null);

        assertNotNull(cc);

        assertNotNull(cc);
        assertNotNull(cc.getBusinessTransactions());
        assertNotNull(cc.getBusinessTransactions().get("abstest"));

        assertEquals("Absolute path test", cc.getBusinessTransactions().get("abstest").getDescription());
    }

}
