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

package org.hawkular.apm.server.api.utils.zipkin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Storage for loading and accessing binary annotations mappings.
 *
 * @author Pavol Loffay
 */
public class BinaryAnnotationMappingStorage {

    private static Logger log = Logger.getLogger(BinaryAnnotationMappingStorage.class.getName());

    private static final String HAWKULAR_ZIPKIN_BINARY_ANNOTATION_MAPPING = "zipkin-binary-annotation-mapping.json";

    private Map<String, BinaryAnnotationMapping> keyBasedMappings;

    /**
     * Default storage. It loads mappings from jboss configuration directory.
     */
    public BinaryAnnotationMappingStorage() {

        String jbossConfigDir = System.getProperties().getProperty("jboss.server.config.dir");
        if (jbossConfigDir == null) {
            log.errorf("Property jboss.server.config.dir is not set, Binary Annotation mapping rules set to empty");
            keyBasedMappings = Collections.emptyMap();
            return;
        }

        String path = System.getProperty("jboss.server.config.dir") + File.separatorChar +
                HAWKULAR_ZIPKIN_BINARY_ANNOTATION_MAPPING;

        loadMappings(path);
    }

    /**
     * Storage which loads mapping from custom file
     *
     * @param file path to mapping file. Path have to contain mapping file name.
     */
    public BinaryAnnotationMappingStorage(String file) {
        loadMappings(file);
    }

    private void loadMappings(String path) {
        try {
            String file = readFile(new File(path));

            TypeReference<Map<String, BinaryAnnotationMapping>> typeReference = new
                    TypeReference<Map<String, BinaryAnnotationMapping>> () {};

            ObjectMapper objectMapper = new ObjectMapper();
            JsonParser parser = objectMapper.getFactory().createParser(file);

            keyBasedMappings = Collections.unmodifiableMap(parser.readValueAs(typeReference));
        } catch (IOException ex) {
            log.errorf("Could not load Zipkin binary annotation mapping file %s", path);
            keyBasedMappings = Collections.emptyMap();
        }
    }

    public Map<String, BinaryAnnotationMapping> getKeyBasedMappings() {
        return keyBasedMappings;
    }

    private String readFile(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(file.toURI()));
        return new String(bytes);
    }
}
