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
package org.hawkular.apm.server.elasticsearch;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.api.model.analytics.BuildStamp;
import org.hawkular.apm.api.model.analytics.Cardinality;
import org.hawkular.apm.api.model.analytics.ServiceDeployment;
import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.api.services.ServiceDeploymentService;

/**
 * The implementation of {@link ServiceDeploymentService} for the Elasticsearch backend.
 *
 * @author Juraci Paixão Kröhling
 */
public class ServiceDeploymentServiceElasticsearch implements ServiceDeploymentService {
    @Inject
    AnalyticsServiceElasticsearch analyticsService;

    /**
     * @see ServiceDeploymentService#getServiceDeployments(String, Criteria)
     */
    @Override
    public List<ServiceDeployment> getServiceDeployments(String tenantId, Criteria criteria) {
        // we create a new service criteria for two reasons:
        // 1) we protect ourselves from null values
        // 2) we will end up adding our own criteria properties, and we don't want to change the
        // object that has been provided to us, as the same criteria might be used later by the caller
        Criteria serviceCriteria = new Criteria(criteria);
        return analyticsService
                .getTraceCompletionPropertyDetails(tenantId, serviceCriteria, Constants.PROP_SERVICE_NAME)
                .stream()
                .map(c -> getService(c.getValue(), tenantId, serviceCriteria))
                .collect(Collectors.toList());
    }

    /**
     * @see ServiceDeploymentService#getServiceDeployments(String)
     */
    @Override
    public List<ServiceDeployment> getServiceDeployments(String tenantId) {
        return getServiceDeployments(tenantId, null);
    }

    /**
     * @see ServiceDeploymentService#getService(String, String, Criteria)
     */
    @Override
    public ServiceDeployment getService(String serviceName, String tenantId, Criteria criteria) {
        // see getServiceDeployments(String, Criteria) for why we clone the criteria here
        Criteria buildStampCriteria = new Criteria(criteria);

        buildStampCriteria.addProperty(Constants.PROP_SERVICE_NAME, serviceName, Criteria.Operator.EQ);
        List<BuildStamp> buildStamps = analyticsService
                .getEndpointPropertyDetails(tenantId, buildStampCriteria, Constants.PROP_BUILD_STAMP)
                .stream()
                .map(this::toBuildStamp)
                .collect(Collectors.toList());

        return new ServiceDeployment(serviceName, buildStamps);
    }

    /**
     * @see ServiceDeploymentService#getService(String, String)
     */
    @Override
    public ServiceDeployment getService(String serviceName, String tenantId) {
        return getService(serviceName, tenantId, null);
    }

    private BuildStamp toBuildStamp(Cardinality cardinality) {
        return new BuildStamp(cardinality.getValue());
    }
}
