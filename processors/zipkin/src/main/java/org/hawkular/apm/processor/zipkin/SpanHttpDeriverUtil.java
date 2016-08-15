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

package org.hawkular.apm.processor.zipkin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Span;

/**
 * @author Pavol Loffay
 */
public class SpanHttpDeriverUtil {
    private static final Logger log = Logger.getLogger(SpanHttpDeriverUtil.class.getName());

    private static final Set<String> HTTP_METHODS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("GET", "PUT", "POST", "DELETE", "HEAD", "TRACE", "OPTIONS")));

    protected static final String ZIPKIN_HTTP_CODE_KEY = "http.status_code";

    private SpanHttpDeriverUtil() {}

    /**
     * Method returns list of http status codes.
     *
     * @param binaryAnnotations zipkin binary annotations
     * @return http status codes
     */
    public static List<HttpCode> getHttpStatusCodes(List<BinaryAnnotation> binaryAnnotations) {
        if (binaryAnnotations == null) {
            return Collections.emptyList();
        }

        List<HttpCode> httpCodes = new ArrayList<>();

        for (BinaryAnnotation binaryAnnotation: binaryAnnotations) {
            if (ZIPKIN_HTTP_CODE_KEY.equals(binaryAnnotation.getKey()) &&
                    binaryAnnotation.getValue() != null) {

                String strHttpCode = binaryAnnotation.getValue();
                Integer httpCode = toInt(strHttpCode.trim());

                if (httpCode != null) {
                    String description = EnglishReasonPhraseCatalog.INSTANCE.getReason(httpCode, Locale.ENGLISH);
                    httpCodes.add(new HttpCode(httpCode, description));
                }
            }

        }
        return httpCodes;
    }

    /**
     * Method returns only client or sever http errors.
     *
     * @param httpCodes list of http codes
     * @return Http client and server errors
     */
    public static List<HttpCode> getClientOrServerErrors(List<HttpCode> httpCodes) {
        return httpCodes.stream()
                .filter(x -> x.isClientOrServerError())
                .collect(Collectors.toList());
    }

    /**
     * Derives HTTP operation from Span's binary annotations.
     *
     * @param span the span
     * @return HTTP method
     */
    public static String getHttpMethod(Span span) {
        List<SpanHttpDeriverUtil.HttpCode> httpStatusCodes =
                SpanHttpDeriverUtil.getHttpStatusCodes(span.getBinaryAnnotations());

        if (httpStatusCodes.size() > 0) {

            String httpMethod = span.getName().toUpperCase();

            if (HTTP_METHODS.contains(httpMethod)) {
                return httpMethod;
            }
        }

        return null;
    }

    /**
     * Converts string to a number.
     *
     * @param str The string
     * @return number or null if conversion failed
     */
    private static Integer toInt(String str) {

        Integer num = null;
        try {
            num = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            log.severe(String.format("failed to convert str: %s to integer", str));
        }

        return num;
    }

    public static class HttpCode {
        private int code;
        private String description;

        public HttpCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public boolean isClientOrServerError() {
            return code >= 400;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HttpCode)) return false;

            HttpCode httpCode = (HttpCode) o;

            return code == httpCode.code;

        }

        @Override
        public int hashCode() {
            return code;
        }
    }
}
