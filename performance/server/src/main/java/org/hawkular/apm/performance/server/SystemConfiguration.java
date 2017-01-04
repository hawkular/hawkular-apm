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
package org.hawkular.apm.performance.server;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gbrown
 */
public class SystemConfiguration {

    private List<ServiceConfiguration> services = new ArrayList<ServiceConfiguration>();

    private List<PathConfiguration> paths = new ArrayList<PathConfiguration>();

    /**
     * @return the services
     */
    public List<ServiceConfiguration> getServices() {
        return services;
    }

    /**
     * @param services the services to set
     */
    public void setServices(List<ServiceConfiguration> services) {
        this.services = services;
    }

    /**
     * @return the paths
     */
    public List<PathConfiguration> getPaths() {
        return paths;
    }

    /**
     * @param paths the paths to set
     */
    public void setPaths(List<PathConfiguration> paths) {
        this.paths = paths;
    }

}
