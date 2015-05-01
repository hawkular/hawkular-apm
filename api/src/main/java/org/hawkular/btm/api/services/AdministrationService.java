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

import org.hawkular.btm.api.model.admin.CollectorConfiguration;

/**
 * This interface represents the service used to manage business transaction
 * administration capabilities.
 *
 * @author gbrown
 */
public interface AdministrationService {

    /**
     * This method returns the business transaction collector configuration
     * associated with the supplied resource.
     *
     * @param resource The resource requesting the configuration
     * @return The collector configuration
     */
    CollectorConfiguration getConfiguration(String resource);

}
