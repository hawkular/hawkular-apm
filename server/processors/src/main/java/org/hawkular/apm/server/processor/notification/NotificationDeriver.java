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
package org.hawkular.apm.server.processor.notification;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.events.Notification;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.server.api.task.AbstractProcessor;
import org.hawkular.apm.server.api.task.RetryAttemptException;

/**
 * This class represents the notification deriver.
 *
 * @author gbrown
 */
public class NotificationDeriver extends AbstractProcessor<Trace, Notification> {

    private static final Logger log = Logger.getLogger(NotificationDeriver.class.getName());

    /**
     * The default constructor.
     */
    public NotificationDeriver() {
        super(ProcessorType.OneToOne);
    }

    @Override
    public Notification processOneToOne(String tenantId, Trace item) throws RetryAttemptException {
        // Check if named txn and has nodes
        if (item.getBusinessTransaction() != null && !item.getBusinessTransaction().trim().isEmpty()
                && !item.getNodes().isEmpty()) {
            Notification notification = new Notification();
            notification.setId(UUID.randomUUID().toString());
            notification.setTraceId(item.getTraceId());
            notification.setFragmentId(item.getFragmentId());
            notification.setBusinessTransaction(item.getBusinessTransaction());
            notification.setTimestamp(item.getTimestamp());
            notification.setHostAddress(item.getHostAddress());
            notification.setHostName(item.getHostName());

            findIssues(item.getNodes(), notification);

            if (!notification.getIssues().isEmpty()) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("NotificationDeriver ret=" + notification);
                }
                return notification;
            }
        }
        return null;
    }

    /**
     * This method checks for issues associated with the nodes and adds
     * them to the supplied notification.
     *
     * @param nodes The nodes
     * @param notification The notification
     */
    protected void findIssues(List<Node> nodes, Notification notification) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (!node.getIssues().isEmpty()) {
                notification.getIssues().addAll(node.getIssues());
            }

            if (node.containerNode()) {
                findIssues(((ContainerNode) node).getNodes(), notification);
            }
        }
    }

}
