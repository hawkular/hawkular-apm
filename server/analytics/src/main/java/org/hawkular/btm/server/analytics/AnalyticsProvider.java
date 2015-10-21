/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.server.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.ContainerNode;
import org.hawkular.btm.api.model.btxn.Node;
import org.hawkular.btm.api.services.AnalyticsService;
import org.hawkular.btm.api.services.BusinessTransactionCriteria;
import org.hawkular.btm.api.services.BusinessTransactionService;

/**
 * This class provides the implementation for the Analytics interface.
 *
 * @author gbrown
 */
public class AnalyticsProvider implements AnalyticsService {

    @Inject
    private BusinessTransactionService businessTransactionService;

    /**
     * @return the businessTransactionService
     */
    public BusinessTransactionService getBusinessTransactionService() {
        return businessTransactionService;
    }

    /**
     * @param businessTransactionService the businessTransactionService to set
     */
    public void setBusinessTransactionService(BusinessTransactionService businessTransactionService) {
        this.businessTransactionService = businessTransactionService;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.services.Analytics#getUnboundURIs(java.lang.String, long, long)
     */
    @Override
    public List<String> getUnboundURIs(String tenantId, long start, long end) {
        BusinessTransactionCriteria criteria = new BusinessTransactionCriteria()
                        .setStartTime(start)
                        .setEndTime(end);

        List<BusinessTransaction> fragments = businessTransactionService.query(tenantId, criteria);

        // Process the fragments to identify which URIs are no used in any business transaction
        Set<String> unboundURIs = new HashSet<String>();
        Set<String> boundURIs = new HashSet<String>();

        for (int i = 0; i < fragments.size(); i++) {
            BusinessTransaction btxn = fragments.get(i);
            analyseURIs(btxn.getName() != null, btxn.getNodes(), unboundURIs, boundURIs);
        }

        // Remove any URIs that may subsequently have become bound
        unboundURIs.removeAll(boundURIs);

        // Convert the set to a sorted list
        List<String> ret = new ArrayList<String>(unboundURIs);

        Collections.sort(ret);

        return ret;
    }

    /**
     * This method collects the information regarding bound and unbound URIs.
     *
     * @param bound Whether the business transaction fragment being processed is bound
     * @param nodes The nodes
     * @param unboundURIs The list of unbound URIs
     * @param boundURIs The list of bound URIs
     */
    protected void analyseURIs(boolean bound, List<Node> nodes, Set<String> unboundURIs,
            Set<String> boundURIs) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);

            if (node.getUri() != null) {
                if (bound) {
                    boundURIs.add(node.getUri());
                } else {
                    unboundURIs.add(node.getUri());
                }
            }

            if (node instanceof ContainerNode) {
                analyseURIs(bound, ((ContainerNode) node).getNodes(),
                        unboundURIs, boundURIs);
            }
        }
    }
}
