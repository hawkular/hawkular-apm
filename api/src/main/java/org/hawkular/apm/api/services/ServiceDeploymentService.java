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
package org.hawkular.apm.api.services;

import java.util.List;

import org.hawkular.apm.api.model.analytics.BuildStamp;
import org.hawkular.apm.api.model.analytics.ServiceDeployment;

/**
 * Provides access to Service Deployment information. The name of this class is a bit unfortunate, but follows the pattern
 * for this kind of class.
 *
 * @author Juraci Paixão Kröhling
 */
public interface ServiceDeploymentService {

    /**
     * Returns the available service deployments. Service here, at this point, are individual services. A service has 1+ buildStamps.
     * The list of buildStamps within the service is complete and is not provided in any specific order. The provided criteria will
     * be used as a base for retrieving the service deployments, but the original object is not changed. Additional criteria is added,
     * so that the results are restricted to service deployments. Note that the same base criteria is passed down to the build stamp
     * retrieval, so, the criteria has to be valid for both.
     *
     * @param tenantId The tenant to get the service deployments from. Optional.
     * @param criteria The base criteria. Optional.
     * @return a List of Service Deployments, or an empty list if none is available.
     */
    List<ServiceDeployment> getServiceDeployments(String tenantId, Criteria criteria);

    /**
     * Same as {@link #getServiceDeployments(String, Criteria)}, without using any extra parameters as base criteria.
     *
     * @see #getServiceDeployments(String, Criteria)
     * @return a List of Service Deployments, or an empty list if none is available.
     */
    List<ServiceDeployment> getServiceDeployments(String tenantId);

    /**
     * Returns a single service deployment, based on the provided service name. This method has the same semantics as
     * {@link #getServiceDeployments(String, Criteria)}, except that the list of {@link BuildStamp} might be empty.
     *
     * @param serviceName The service name. Required.
     * @param tenantId    The tenant to get the service deployments from. Optional.
     * @param criteria    The base criteria. Optional.
     * @return the {@link ServiceDeployment} for the given service name, including {@link BuildStamp}s, if any.
     */
    ServiceDeployment getService(String serviceName, String tenantId, Criteria criteria);

    /**
     * Returns a single service deployment, based on the provided service name. This method has the same semantics as
     * {@link #getServiceDeployments(String, Criteria)}, except that the list of {@link BuildStamp} might be empty.
     *
     * @param serviceName The service name. Required.
     * @param tenantId    The tenant to get the service deployments from. Optional.
     * @return the {@link ServiceDeployment} for the given service name, including {@link BuildStamp}s, if any.
     */
    ServiceDeployment getService(String serviceName, String tenantId);
}
