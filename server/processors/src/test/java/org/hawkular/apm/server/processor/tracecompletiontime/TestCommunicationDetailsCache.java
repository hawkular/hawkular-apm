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
package org.hawkular.apm.server.processor.tracecompletiontime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.events.CommunicationDetails;
import org.hawkular.apm.server.api.services.CommunicationDetailsCache;

/**
 * @author gbrown
 */
public class TestCommunicationDetailsCache implements CommunicationDetailsCache {

    private Map<String, CommunicationDetails> singleConsumer = new HashMap<String, CommunicationDetails>();
    private Map<String, List<CommunicationDetails>> multipleConsumers = new HashMap<String,
                            List<CommunicationDetails>>();

    @Override
    public CommunicationDetails get(String tenantId, String id) {
        return singleConsumer.get(id);
    }

    @Override
    public void store(String tenantId, List<CommunicationDetails> details) {
        for (int i=0; i < details.size(); i++) {
            CommunicationDetails cd=details.get(i);
            if (!cd.isMultiConsumer()) {
                singleConsumer.put(cd.getLinkId(), cd);
            } else {
                List<CommunicationDetails> list = multipleConsumers.get(cd.getLinkId());
                if (list == null) {
                    list = new ArrayList<CommunicationDetails>();
                    multipleConsumers.put(cd.getLinkId(), list);
                }
                list.add(cd);
            }
        }
    }

    @Override
    public List<CommunicationDetails> getById(String tenantId, String id) {
        throw new UnsupportedOperationException();
    }
}
