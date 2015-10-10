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
package org.hawkular.btm.admin.service.inmemory;

import javax.inject.Singleton;

import org.hawkular.btm.api.model.config.CollectorConfiguration;
import org.hawkular.btm.api.services.AdminService;
import org.hawkular.btm.api.services.ConfigurationLoader;

/**
 * This class provides the in-memory implementation of the Admin
 * Service. This implementation is only intended for testing usage.
 *
 * @author gbrown
 */
@Singleton
public class AdminServiceInMemory implements AdminService {

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.services.AdminService#getConfiguration(java.lang.String,
     *                          java.lang.String, java.lang.String)
     */
    @Override
    public CollectorConfiguration getConfiguration(String tenantId, String host, String server) {
        return ConfigurationLoader.getConfiguration();
    }

}
