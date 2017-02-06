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

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;

/**
 * Singleton class for deriving information from binary annotations.
 *
 * @author Pavol Loffay
 */
public class BinaryAnnotationMappingDeriver {

    private static Object LOCK = new Object();

    private static BinaryAnnotationMappingDeriver instance;

    private final BinaryAnnotationMappingStorage mappingStorage;


    private BinaryAnnotationMappingDeriver() {
        mappingStorage = new BinaryAnnotationMappingStorage();
    }

    private BinaryAnnotationMappingDeriver(String path) {
        mappingStorage = new BinaryAnnotationMappingStorage(path);
    }

    /**
     * Returns default instance of mapping deriver.
     *
     * @return binary annotation mapping deriver
     */
    public static BinaryAnnotationMappingDeriver getInstance() {
        return getInstance(null);
    }

    /**
     * Returns instance of mapping deriver.
     *
     * @param path path to the mapping file
     * @return binary annotation mapping deriver
     */
    public static BinaryAnnotationMappingDeriver getInstance(String path) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = path == null ? new BinaryAnnotationMappingDeriver() :
                            new BinaryAnnotationMappingDeriver(path);
                }
            }
        }

        return instance;
    }

    /**
     * Creates a mapping result from supplied binary annotations.
     *
     * @param binaryAnnotations binary annotations  of span
     * @return mapping result
     */
    public MappingResult mappingResult(List<BinaryAnnotation> binaryAnnotations) {
        if (binaryAnnotations == null) {
            return new MappingResult();
        }

        List<String> componentTypes = new ArrayList<>();
        List<String> endpointTypes = new ArrayList<>();

        MappingResult.Builder mappingBuilder = MappingResult.builder();

        for (BinaryAnnotation binaryAnnotation: binaryAnnotations) {
            if (binaryAnnotation.getKey() == null) {
                continue;
            }

            BinaryAnnotationMapping mapping = mappingStorage.getKeyBasedMappings().get(binaryAnnotation.getKey());
            if (mapping != null && mapping.isIgnore()) {
                continue;
            }

            if (mapping == null || mapping.getProperty() == null) {
                // If no mapping, then just store property
                mappingBuilder.addProperty(new Property(binaryAnnotation.getKey(), binaryAnnotation.getValue(),
                        AnnotationTypeUtil.toPropertyType(binaryAnnotation.getType())));
            }

            if (mapping != null) {
                if (mapping.getComponentType() != null) {
                    componentTypes.add(mapping.getComponentType());
                }

                if (mapping.getEndpointType() != null) {
                    endpointTypes.add(mapping.getEndpointType());
                }

                if (mapping.getProperty() != null && !mapping.getProperty().isExclude()) {
                    String key = mapping.getProperty().getKey() != null ? mapping.getProperty().getKey() :
                            binaryAnnotation.getKey();
                    mappingBuilder.addProperty(new Property(key, binaryAnnotation.getValue(),
                            AnnotationTypeUtil.toPropertyType(binaryAnnotation.getType())));
                }
            }
        }

        if (!componentTypes.isEmpty()) {
            mappingBuilder.withComponentType(componentTypes.get(0));
        }
        if (!endpointTypes.isEmpty()) {
            mappingBuilder.withEndpointType(endpointTypes.get(0));
        }

        return mappingBuilder.build();
    }

    /**
     * Clears the storage so it will load mapping again
     */
    public static void clearStorage() {
        synchronized (LOCK) {
            instance = null;
        }
    }

}
