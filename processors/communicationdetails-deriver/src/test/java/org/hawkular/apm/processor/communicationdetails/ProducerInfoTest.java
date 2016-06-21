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
package org.hawkular.apm.processor.communicationdetails;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.PropertyType;
import org.junit.Test;

/**
 * @author gbrown
 */
public class ProducerInfoTest {

    @Test
    public void testSerialize() {
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

        ProducerInfo pi = new ProducerInfo();
        pi.setId("testId");
        pi.setDuration(500);
        pi.setFragmentId("fragId");
        pi.setHostAddress("hostAddr");
        pi.setHostName("hostName");
        pi.setMultipleConsumers(true);
        pi.setSourceOperation("sourceOp");
        pi.setSourceUri("sourceUri");
        pi.setProperties(props);
        pi.setTimestamp(System.currentTimeMillis());

        ProducerInfo result = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(pi);

            oos.flush();
            oos.close();
            baos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            result = (ProducerInfo) ois.readObject();

            bais.close();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to serialize: " + e);
        }

        assertNotNull(result);

        assertEquals(result, pi);
    }

    @Test
    public void testSerializeAllNullStringFields() {
        Set<Property> props = new HashSet<Property>();
        Property prop1 = new Property();
        prop1.setType(PropertyType.Text);
        props.add(prop1);

        ProducerInfo pi = new ProducerInfo();
        pi.setProperties(props);
        pi.setTimestamp(System.currentTimeMillis());

        ProducerInfo result = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(pi);

            oos.flush();
            oos.close();
            baos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);

            result = (ProducerInfo) ois.readObject();

            bais.close();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to serialize: " + e);
        }

        assertNotNull(result);

        assertEquals(result, pi);
    }

}
