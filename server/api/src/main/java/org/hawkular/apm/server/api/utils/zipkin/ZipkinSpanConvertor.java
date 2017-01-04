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

package org.hawkular.apm.server.api.utils.zipkin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

import org.hawkular.apm.server.api.model.zipkin.Annotation;
import org.hawkular.apm.server.api.model.zipkin.AnnotationType;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.hawkular.apm.server.api.model.zipkin.Endpoint;
import org.hawkular.apm.server.api.model.zipkin.Span;

import zipkin.internal.ApplyTimestampAndDuration;

/**
 * @author Pavol Loffay
 */
public class ZipkinSpanConvertor {

    private ZipkinSpanConvertor() {}

    public static Span span(zipkin.Span zipkinSpan) {
        zipkinSpan = ApplyTimestampAndDuration.apply(zipkinSpan);

        Span span = new Span(binaryAnnotations(zipkinSpan.binaryAnnotations),
                annotations(zipkinSpan.annotations));

        span.setId(parseSpanId(zipkinSpan.id));
        span.setTraceId(parseSpanId(zipkinSpan.traceId));
        span.setParentId(zipkinSpan.parentId == null ? null : parseSpanId(zipkinSpan.parentId));

        span.setName(zipkinSpan.name);
        span.setDebug(zipkinSpan.debug);

        span.setTimestamp(zipkinSpan.timestamp);
        /**
         * TODO This avoids NPE in zipkin processors, it should be removed in future
         */
        span.setDuration(zipkinSpan.duration == null ? 0 : zipkinSpan.duration);

        return span;
    }

    public static Endpoint endpoint(zipkin.Endpoint zipkinEndpoint) {
        if (zipkinEndpoint == null) {
            return null;
        }

        Endpoint endpoint = new Endpoint();
        endpoint.setServiceName(zipkinEndpoint.serviceName);
        endpoint.setIpv4(parseIpv4(zipkinEndpoint.ipv4));
        endpoint.setPort(zipkinEndpoint.port);
        return endpoint;
    }

    public static Annotation annotation(zipkin.Annotation zipkinAnnotation) {
        Annotation annotation = new Annotation();
        annotation.setValue(zipkinAnnotation.value);
        annotation.setTimestamp(zipkinAnnotation.timestamp);
        annotation.setEndpoint(endpoint(zipkinAnnotation.endpoint));
        return annotation;
    }

    public static BinaryAnnotation binaryAnnotation(zipkin.BinaryAnnotation zipkinBinaryAnnotation) {
        BinaryAnnotation binaryAnnotation = new BinaryAnnotation();
        binaryAnnotation.setKey(zipkinBinaryAnnotation.key);
        binaryAnnotation.setValue(parseBinaryAnnotationValue(zipkinBinaryAnnotation));
        binaryAnnotation.setType(annotationType(zipkinBinaryAnnotation.type));
        binaryAnnotation.setEndpoint(endpoint(zipkinBinaryAnnotation.endpoint));

        return binaryAnnotation;
    }

    public static AnnotationType annotationType(zipkin.BinaryAnnotation.Type zipkinType) {

        AnnotationType annotationType;
        switch (zipkinType) {
            case BOOL: annotationType = AnnotationType.BOOL;
                break;
            case STRING: annotationType = AnnotationType.STRING;
                break;
            case DOUBLE: annotationType = AnnotationType.DOUBLE;
                break;
            case BYTES: annotationType = AnnotationType.BYTES;
                break;
            case I16: annotationType = AnnotationType.I16;
                break;
            case I32: annotationType = AnnotationType.I32;
                break;
            case I64: annotationType = AnnotationType.I64;
                break;
            default:
                throw new IllegalArgumentException("Wrong zipkin annotation type");
        }

        return annotationType;
    }

    public static List<Annotation> annotations(List<zipkin.Annotation> zipkinAnnotations) {
        if (zipkinAnnotations == null) {
            return null;
        }
        return zipkinAnnotations.stream()
                .map(ZipkinSpanConvertor::annotation)
                .collect(Collectors.toList());
    }

    public static List<BinaryAnnotation> binaryAnnotations(List<zipkin.BinaryAnnotation> zipkinBinAnnotations) {
        if (zipkinBinAnnotations == null) {
            return null;
        }
        return zipkinBinAnnotations.stream()
                .map(ZipkinSpanConvertor::binaryAnnotation)
                .collect(Collectors.toList());
    }

    public static List<Span> spans(List<zipkin.Span> zipkinSpans) {
        return zipkinSpans.stream()
                .map(ZipkinSpanConvertor::span)
                .collect(Collectors.toList());
    }

    private static String parseBinaryAnnotationValue(zipkin.BinaryAnnotation zipkinBinAnnotation) {
        if (zipkinBinAnnotation == null || zipkinBinAnnotation.value == null) {
            return null;
        }

        String value = null;
        switch (zipkinBinAnnotation.type) {
            case BOOL:
                value = zipkinBinAnnotation.value[0] == 1 ? "true" : "false";
                break;
            case STRING:
                value = new String(zipkinBinAnnotation.value);
                break;
            case BYTES:
                value = String.valueOf(zipkinBinAnnotation.value);
                break;
            case I16:
                value = String.valueOf(ByteBuffer.wrap(zipkinBinAnnotation.value).getShort());
                break;
            case I32:
                value = String.valueOf(ByteBuffer.wrap(zipkinBinAnnotation.value).getInt());
                break;
            case I64:
                value = String.valueOf(ByteBuffer.wrap(zipkinBinAnnotation.value).getLong());
                break;
            case DOUBLE:
                String.valueOf(Double.longBitsToDouble(ByteBuffer.wrap(zipkinBinAnnotation.value).getLong()));
                break;
            default:
                value = null;
        }

        return value;
    }

    private static String parseIpv4(int ipv4) {
        if (ipv4 == 0) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(ipv4 >> 24 & 0xff).append(".");
        stringBuilder.append(ipv4 >> 16 & 0xff).append(".");
        stringBuilder.append(ipv4 >> 8 & 0xff).append(".");
        stringBuilder.append(ipv4  & 0xff);

        return stringBuilder.toString();
    }

    public static String parseSpanId(long id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hex((byte) ((id >>> 56L) & 0xff)));
        stringBuilder.append(hex((byte) ((id >>> 48L) & 0xff)));
        stringBuilder.append(hex((byte) ((id >>> 40L) & 0xff)));
        stringBuilder.append(hex((byte) ((id >>> 32L) & 0xff)));
        stringBuilder.append(hex((byte) ((id >>> 24L) & 0xff)));
        stringBuilder.append(hex((byte) ((id >>> 16L) & 0xff)));
        stringBuilder.append(hex((byte) ((id >>> 8L) & 0xff)));
        stringBuilder.append(hex((byte) (id & 0xff)));

        return stringBuilder.toString();
    }

    private static String hex(byte b) {
        return String.format("%02X", b);
    }
}
