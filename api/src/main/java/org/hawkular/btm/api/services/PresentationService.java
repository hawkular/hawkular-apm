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
package org.hawkular.btm.api.services;

import java.util.Map;

import org.hawkular.btm.api.model.Level;
import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.api.model.btxn.CorrelationIdentifier;

/**
 * This interface represents the service used to provide presentation
 * support for business transaction management.
 *
 * @author gbrown
 */
public interface PresentationService {

    /**
     * This method returns an instance or aggregated group view of a business transaction
     * based on the correlation identifier and/or business properties specified.
     *
     * @param levels The level of informations to be presented
     * @param ids The correlation ids
     * @param properties The business properties
     * @return The instance or aggregated group view of the business transaction
     */
    BusinessTransaction getBusinessTransaction(Level[] levels, CorrelationIdentifier[] ids,
            Map<String, String> properties);

}
