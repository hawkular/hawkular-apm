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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.CollectorConfiguration;
import org.hawkular.apm.api.utils.PropertyUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the capability to load the base collector configuration
 * which includes the instrumentation rules for various technologies.
 *
 * @author gbrown
 */
public class ConfigurationLoader {

    private static final String DEFAULT_TYPE = "jvm";

    /** The system property that optional contains the location of the configuration */
    public static final String HAWKULAR_APM_CONFIG = "HAWKULAR_APM_CONFIG";

    private static final String DEFAULT_URI = "apmconfig";

    private static ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = Logger.getLogger(ConfigurationLoader.class.getName());

    /**
     * This method returns the collector configuration.
     *
     * @param type The type, or null if default (jvm)
     * @return The collection configuration
     */
    public static CollectorConfiguration getConfiguration(String type) {
        return loadConfig(PropertyUtil.getProperty(HAWKULAR_APM_CONFIG, DEFAULT_URI), type);
    }

    /**
     * This method loads the configuration from the supplied URI.
     *
     * @param uri The URI
     * @param type The type, or null if default (jvm)
     * @return The configuration
     */
    protected static CollectorConfiguration loadConfig(String uri, String type) {
        final CollectorConfiguration config = new CollectorConfiguration();

        if (type == null) {
            type = DEFAULT_TYPE;
        }

        uri += java.io.File.separator + type;

        File f = new File(uri);

        if (!f.isAbsolute()) {
            if (f.exists()) {
                uri = f.getAbsolutePath();
            } else if (System.getProperties().containsKey("jboss.server.config.dir")) {
                uri = System.getProperty("jboss.server.config.dir") + java.io.File.separatorChar + uri;
            } else {
                try {
                    URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
                    if (url != null) {
                        uri = url.getPath();
                    } else {
                        log.severe("Failed to get absolute path for uri '" + uri + "'");
                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Failed to get absolute path for uri '" + uri + "'", e);
                    uri = null;
                }
            }
        }

        if (uri != null) {
            String[] uriParts = uri.split(Matcher.quoteReplacement(File.separator));
            int startIndex = 0;

            // Remove any file prefix
            if (uriParts[0].equals("file:")) {
                startIndex++;
            }

            try {
                Path path = getPath(startIndex, uriParts);

                Files.walkFileTree(path, new FileVisitor<Path>() {

                    @Override
                    public FileVisitResult postVisitDirectory(Path path, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        if (path.toString().endsWith(".json")) {
                            String json = new String(Files.readAllBytes(path));
                            CollectorConfiguration childConfig = mapper.readValue(json, CollectorConfiguration.class);
                            if (childConfig != null) {
                                config.merge(childConfig, false);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                });
            } catch (Throwable e) {
                log.log(Level.SEVERE, "Failed to load configuration", e);
            }
        }

        return config;
    }

    /**
     * This method constructs a path based on potentially accessing
     * one or more archive files (jar/war).
     *
     * @param startindex The start index
     * @param uriParts The parts of the URI
     * @return The path
     */
    protected static Path getPath(int startindex, String[] uriParts) {
        Path ret = Paths.get("/");
        List<FileSystem> toClose = new ArrayList<FileSystem>();

        try {
            for (int i = startindex; i < uriParts.length; i++) {
                String name = uriParts[i];
                if (name.endsWith("!")) {
                    name = name.substring(0, name.length() - 1);
                }
                ret = ret.resolve(name);

                if (name.endsWith(".jar") || name.endsWith(".war")) {
                    try (FileSystem jarfs = FileSystems.newFileSystem(ret,
                            Thread.currentThread().getContextClassLoader())) {
                        ret = jarfs.getRootDirectories().iterator().next();
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "Failed to access archive '" + name + "'", e);
                    }
                }
            }
        } finally {
            for (FileSystem fs : toClose) {
                try {
                    fs.close();
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Failed to close file system '" + fs + "'", e);
                }
            }
        }

        return ret;
    }

}
