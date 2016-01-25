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
package org.hawkular.btm.processor.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.hawkular.btm.api.model.Severity;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.Component;
import org.hawkular.btm.api.model.btxn.Consumer;
import org.hawkular.btm.api.model.btxn.ProcessorIssue;
import org.hawkular.btm.api.model.events.Notification;
import org.junit.Test;

/**
 * @author gbrown
 */
public class NotificationDeriverTest {

    @Test
    public void testDeriveNotificationWithRecuriveScanForIssues() {
        NotificationDeriver deriver = new NotificationDeriver();

        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setId("myid");
        btxn.setName("mybtxn");
        btxn.setStartTime(1000);
        btxn.setHostAddress("myhostaddr");
        btxn.setHostName("myhostname");

        ProcessorIssue issue1 = new ProcessorIssue();
        issue1.setAction("a1");
        issue1.setDescription("d1");
        issue1.setSeverity(Severity.Error);
        issue1.setProcessor("p1");

        ProcessorIssue issue2 = new ProcessorIssue();
        issue2.setAction("a2");
        issue2.setDescription("d2");
        issue2.setSeverity(Severity.Warning);
        issue2.setProcessor("p2");

        ProcessorIssue issue3 = new ProcessorIssue();
        issue3.setAction("a3");
        issue3.setDescription("d3");
        issue3.setSeverity(Severity.Info);
        issue3.setProcessor("p3");

        Consumer c = new Consumer();
        c.getIssues().add(issue1);
        btxn.getNodes().add(c);

        Component comp = new Component();
        comp.getIssues().add(issue2);
        comp.getIssues().add(issue3);
        c.getNodes().add(comp);

        Notification notification = null;
        try {
            notification = deriver.processSingle(btxn);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to process notifications: " + e);
        }

        assertNotNull(notification);
        assertEquals(btxn.getId(), notification.getId());
        assertEquals(btxn.getName(), notification.getBusinessTransaction());
        assertEquals(btxn.getStartTime(), notification.getTimestamp());
        assertEquals(btxn.getHostAddress(), notification.getHostAddress());
        assertEquals(btxn.getHostName(), notification.getHostName());

        assertEquals(3, notification.getIssues().size());

        assertEquals(issue1, notification.getIssues().get(0));
        assertEquals(issue2, notification.getIssues().get(1));
        assertEquals(issue3, notification.getIssues().get(2));
    }

    @Test
    public void testDeriveNotificationNoIssues() {
        NotificationDeriver deriver = new NotificationDeriver();

        BusinessTransaction btxn = new BusinessTransaction();
        btxn.setId("myid");
        btxn.setName("mybtxn");
        btxn.setStartTime(1000);
        btxn.setHostAddress("myhostaddr");
        btxn.setHostName("myhostname");

        Consumer c = new Consumer();
        btxn.getNodes().add(c);

        Component comp = new Component();
        c.getNodes().add(comp);

        Notification notification = null;
        try {
            notification = deriver.processSingle(btxn);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to process notifications: " + e);
        }

        assertNull(notification);
    }

}
