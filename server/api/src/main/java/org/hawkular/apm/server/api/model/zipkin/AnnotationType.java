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
package org.hawkular.apm.server.api.model.zipkin;

public enum AnnotationType {
    BOOL(0), BYTES(1), I16(2), I32(3), I64(4), DOUBLE(5), STRING(6);

    private final int value;

    AnnotationType(int value) {
        this.value = value;
    }

    /**
     * Get the integer value of this enum value, as defined in the Thrift IDL.
     */
    public int getValue() {
        return value;
    }

    /** Returns {@link AnnotationType#BYTES} if unknown. */
    public static AnnotationType fromValue(int value) {
        switch (value) {
            case 0:
                return BOOL;
            case 1:
                return BYTES;
            case 2:
                return I16;
            case 3:
                return I32;
            case 4:
                return I64;
            case 5:
                return DOUBLE;
            case 6:
                return STRING;
            default:
                return BYTES;
        }
    }
}
