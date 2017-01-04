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
package org.hawkular.apm.server.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.hawkular.apm.api.services.Criteria;
import org.hawkular.apm.server.api.security.SecurityProvider;
import org.hawkular.apm.server.api.security.SecurityProviderException;
import org.hawkular.apm.server.rest.entity.CriteriaRequest;
import org.hawkular.apm.server.rest.entity.TenantRequest;
import org.jboss.logging.Logger;

/**
 * @author Juraci Paixão Kröhling
 */
abstract class BaseHandler {
    private static final Logger log = Logger.getLogger(BaseHandler.class);

    @Inject
    SecurityProvider securityProvider;

    <T> Response withCriteria(CriteriaRequest request, BiFunction<Criteria, String, T> function) {
        return withErrorHandler(() -> {
            String tenant = getTenant(request);
            Criteria criteria = request.toCriteria();
            log.tracef("Get results for criteria [%s]", criteria);
            T entity = function.apply(request.toCriteria(), tenant);
            log.tracef("Got results for criteria [%s] = %s", criteria, entity);

            return Response
                    .status(Response.Status.OK)
                    .entity(entity)
                    .type(APPLICATION_JSON_TYPE)
                    .build();

        });
    }

    Response withErrorHandler(Callable<Response> callable) {
        try {
            return callable.call();
        } catch (Throwable t) {
            t.printStackTrace();
            log.debug(t.getMessage(), t);
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("errorMsg", "Internal Error: " + t.getMessage()))
                    .type(APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    Response clearRequest(Callable<Void> clearOperation) {
        return withErrorHandler(() -> {
            if (System.getProperties().containsKey("hawkular-apm.testmode")) {
                clearOperation.call();
                return Response
                        .status(Response.Status.OK)
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            } else {
                return Response
                        .status(Response.Status.FORBIDDEN)
                        .type(APPLICATION_JSON_TYPE)
                        .build();
            }
        });
    }

    String getTenant(TenantRequest request) throws SecurityProviderException {
        return securityProvider.validate(request.getTenantId(), request.getContext().getUserPrincipal().getName());
    }

}
