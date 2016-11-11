/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

package org.hawkular.apm.server.rest.filter;

import javax.annotation.PostConstruct;
import javax.ws.rs.ext.Provider;

import org.hawkular.apm.api.utils.PropertyUtil;
import org.hawkular.jaxrs.filter.cors.AbstractCorsResponseFilter;
import org.hawkular.jaxrs.filter.cors.AbstractOriginValidation;
import org.hawkular.jaxrs.filter.cors.Headers;

/**
 * @author Pavol Loffay
 */
@Provider
public class CorsFilter extends AbstractCorsResponseFilter {

    private OriginValidation originValidation;
    private String extraAllowedHeaders;

    @PostConstruct
    public void initAllowedOrigins() {
        originValidation = new OriginValidation(
                PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_CORS_ALLOWED_ORIGINS));

        extraAllowedHeaders = "authorization";
        String headers = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_CORS_ACCESS_CONTROL_ALLOW_HEADERS);
        if (headers != null) {
            extraAllowedHeaders += "," + headers;
        }
    }

    @Override
    protected boolean isAllowedOrigin(String origin) {
        return originValidation.isAllowedOrigin(origin);
    }

    @Override
    protected String getExtraAccessControlAllowHeaders() {
        return extraAllowedHeaders;
    }

    private static final class OriginValidation extends AbstractOriginValidation {
        private final String allowedCorsOrigins;

        OriginValidation(String allowedCorsOrigins) {
            this.allowedCorsOrigins = allowedCorsOrigins == null ? Headers.ALLOW_ALL_ORIGIN : allowedCorsOrigins;
            init();
        }

        @Override protected String getAllowedCorsOrigins() {
            return allowedCorsOrigins;
        }
    }
}
