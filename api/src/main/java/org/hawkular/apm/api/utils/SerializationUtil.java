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
package org.hawkular.apm.api.utils;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author gbrown
 */
public class SerializationUtil {

    /**
     * This method is used to serialize a potentially null string.
     *
     * @param oos The object output stream
     * @param text The (possibly null) string value
     * @throws IOException Failed to serialize
     */
    public static void serializeString(ObjectOutput oos, String text) throws IOException {
        oos.writeBoolean(text != null);
        if (text != null) {
            oos.writeUTF(text);
        }
    }

    /**
     * This method deserializes a potentially null string value.
     *
     * @param ois The object input stream
     * @return The string value (which may be null)
     * @throws IOException Failed to deserialize
     */
    public static String deserializeString(ObjectInput ois) throws IOException {
        if (ois.readBoolean()) {
            return ois.readUTF();
        }
        return null;
    }

}
