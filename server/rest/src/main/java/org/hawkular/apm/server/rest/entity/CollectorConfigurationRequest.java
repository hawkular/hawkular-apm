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
package org.hawkular.apm.server.rest.entity;

import javax.ws.rs.QueryParam;

import io.swagger.annotations.ApiParam;

/**
 * @author Juraci Paixão Kröhling
 */
public class CollectorConfigurationRequest extends TenantRequest {
    @ApiParam(value = "optional type")
    @QueryParam("type")
    String type;

    @ApiParam(value = "optional host name")
    @QueryParam("host")
    String host;

    @ApiParam(value = "optional server name")
    @QueryParam("server")
    String server;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}
