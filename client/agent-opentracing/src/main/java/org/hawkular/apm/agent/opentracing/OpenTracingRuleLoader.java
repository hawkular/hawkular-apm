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
package org.hawkular.apm.agent.opentracing;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.jboss.byteman.agent.Retransformer;

/**
 * This class provides the ByteMan manager implementation for APM.
 *
 * @author gbrown
 */
public class OpenTracingRuleLoader {

    private static final Logger log = Logger.getLogger(OpenTracingRuleLoader.class.getName());

    private static final String RULE_FILE_EXTENSION = ".btm";
    private static final String ROOT_RULE_FOLDER = "/rules";

    private static Retransformer transformer;

    /**
     * This method initializes the manager.
     *
     * @param trans The ByteMan retransformer
     * @throws Exception
     */
    public static void initialize(Retransformer trans) throws Exception {
        transformer = trans;

        URI uri = OpenTracingRuleLoader.class.getResource(ROOT_RULE_FOLDER).toURI();

        String s = uri.toString();
        int separator = s.indexOf("!/");
        String entryName = s.substring(separator + 2);
        URI fileURI = URI.create(s.substring(0, separator));

        // Load BTM rules and configure agent
        List<String> scripts = new ArrayList<>();
        List<String> scriptNames = new ArrayList<>();

        try (FileSystem fs = FileSystems.newFileSystem(fileURI,
            Collections.<String, Object>emptyMap())) {
            Path rules = fs.getPath(entryName);

            Files.walk(rules).filter(f -> f.toString().endsWith(RULE_FILE_EXTENSION)).forEach(f -> {
                try {
                    scripts.add(new String(Files.readAllBytes(f)));
                    scriptNames.add(f.toString());
                } catch (IOException ioe) {
                    log.log(Level.SEVERE, "Failed to load rule file: " + f.toString(), ioe);
                }
            });
        }

        try (PrintWriter writer = new PrintWriter(new StringWriter())) {
            transformer.installScript(scripts, scriptNames, writer);
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Installed rules");
        }
    }

}
