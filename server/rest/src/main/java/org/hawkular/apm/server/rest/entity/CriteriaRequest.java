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

import java.io.IOException;

import javax.ws.rs.QueryParam;

import org.hawkular.apm.api.services.Criteria;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Defines a request that supplies Criteria
 *
 * @author Juraci Paixão Kröhling
 */
public class CriteriaRequest extends TenantRequest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @QueryParam("criteria")
    String criteria;

    public Criteria toCriteria() throws IOException {
        if (null == criteria || criteria.isEmpty()) {
            return new Criteria();
        }
        return mapper.readValue(this.criteria, Criteria.class);
    }
}
