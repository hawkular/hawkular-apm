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

import org.hawkular.btm.api.model.config.CollectorConfiguration;

/**
 * This interface provides the administration capabilities.
 *
 * @author gbrown
 */
public interface AdminService {

    /**
     * This method returns the collector configuration, used by the
     * collector within an execution environment to instrument and filter
     * information to be reported to the server, based on the optional
     * host and server names.
     *
     * @param tenantId The optional tenant id
     * @param host The optional host name
     * @param server The optional server name
     * @return The collector configuration
     */
    CollectorConfiguration getConfiguration(String tenantId, String host, String server);

}
