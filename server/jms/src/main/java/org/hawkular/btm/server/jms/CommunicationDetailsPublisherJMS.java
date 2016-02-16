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
package org.hawkular.btm.server.jms;

import java.util.List;

import javax.inject.Singleton;

import org.hawkular.btm.api.model.events.CommunicationDetails;
import org.hawkular.btm.server.api.services.CommunicationDetailsPublisher;

/**
 * This class represents the communication details JMS publisher.
 *
 * @author gbrown
 */
@Singleton
public class CommunicationDetailsPublisherJMS extends AbstractPublisherJMS<CommunicationDetails>
        implements CommunicationDetailsPublisher {

    /**  */
    private static final int MAX_RETRIES = 3;
    private static final String DESTINATION = "java:/CommunicationDetails";

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.jms.AbstractPublisherJMS#getDestinationURI()
     */
    @Override
    protected String getDestinationURI() {
        return DESTINATION;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.services.NodeDetailsPublisher#publish(java.lang.String, java.util.List)
     */
    @Override
    public void publish(String tenantId, List<CommunicationDetails> rts) throws Exception {
        doPublish(tenantId, rts, MAX_RETRIES);
    }

}
