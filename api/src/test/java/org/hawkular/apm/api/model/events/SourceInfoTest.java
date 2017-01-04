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
package org.hawkular.apm.api.model.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.PropertyType;
import org.junit.Test;

/**
 * @author gbrown
 */
public class SourceInfoTest {

    @Test
    public void testSerialize() throws IOException, ClassNotFoundException {
        Set<Property> props = new HashSet<Property>();
        Property prop1 = new Property();
        prop1.setName("testProp1");
        prop1.setValue("testValue1");
        prop1.setType(PropertyType.Text);
        props.add(prop1);

        Property prop2 = new Property();
        prop2.setName("testProp2");
        prop2.setValue("testValue2");
        prop2.setType(PropertyType.Number);
        props.add(prop2);

        SourceInfo si = new SourceInfo();
        si.setId("testId");
        si.setDuration(500);
        si.setTraceId("traceId");
        si.setFragmentId("fragId");
        si.setHostAddress("hostAddr");
        si.setHostName("hostName");
        si.setMultipleConsumers(true);
        si.setEndpoint(new EndpointRef("sourceUri", "sourceOp", false));
        si.setProperties(props);
        si.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));

        SourceInfo result = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(si);

        oos.flush();
        oos.close();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        result = (SourceInfo) ois.readObject();

        bais.close();
        ois.close();

        assertNotNull(result);

        assertEquals(result, si);
    }

    @Test
    public void testSerializeAllNullStringFields() throws IOException, ClassNotFoundException {
        Set<Property> props = new HashSet<Property>();
        Property prop1 = new Property();
        prop1.setType(PropertyType.Text);
        props.add(prop1);

        SourceInfo si = new SourceInfo();
        si.setProperties(props);
        si.setTimestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()));

        SourceInfo result = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(si);

        oos.flush();
        oos.close();
        baos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        result = (SourceInfo) ois.readObject();

        bais.close();
        ois.close();

        assertNotNull(result);

        assertEquals(result, si);
    }

}
