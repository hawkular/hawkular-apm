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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hawkular.apm.api.model.Constants;
import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class SpanHttpDeriverUtilTest {

    @Test
    public void testGetHttpStatusCodes() {
        BinaryAnnotation http200 = createHttpCodeBinaryAnnotation("200");
        BinaryAnnotation http302 = createHttpCodeBinaryAnnotation("302");
        BinaryAnnotation http400 = createHttpCodeBinaryAnnotation("400");
        BinaryAnnotation http500 = createHttpCodeBinaryAnnotation("500");

        BinaryAnnotation other = new BinaryAnnotation();
        other.setKey("return code");
        other.setValue("450");

        List<BinaryAnnotation> binaryAnnotationList = Arrays.asList(http200, http302, http400, http500, other);
        Assert.assertEquals(
                Arrays.asList(new SpanHttpDeriverUtil.HttpCode(200, "ok"),
                    new SpanHttpDeriverUtil.HttpCode(302, "302"),
                    new SpanHttpDeriverUtil.HttpCode(400, "400"),
                    new SpanHttpDeriverUtil.HttpCode(500, "500")),
                SpanHttpDeriverUtil.getHttpStatusCodes(binaryAnnotationList));

        binaryAnnotationList = Arrays.asList(other, other);
        Assert.assertEquals(Collections.emptyList(), SpanHttpDeriverUtil.getHttpStatusCodes(binaryAnnotationList));

        Assert.assertEquals("OK", SpanHttpDeriverUtil.getHttpStatusCodes(
                Arrays.asList(createHttpCodeBinaryAnnotation("200"))).get(0).getDescription());
    }

    @Test
    public void testGetHttpStatusCodesOfNullAndEmptyInput() {
        Assert.assertTrue(SpanHttpDeriverUtil.getHttpStatusCodes(null).isEmpty());
        Assert.assertTrue(SpanHttpDeriverUtil.getHttpStatusCodes(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testGetErrors() {
        SpanHttpDeriverUtil.HttpCode code200 = new SpanHttpDeriverUtil.HttpCode(200, "Ok");
        SpanHttpDeriverUtil.HttpCode code302 = new SpanHttpDeriverUtil.HttpCode(302, "Found");
        SpanHttpDeriverUtil.HttpCode code400 = new SpanHttpDeriverUtil.HttpCode(400, "Bad Request");
        SpanHttpDeriverUtil.HttpCode code501 = new SpanHttpDeriverUtil.HttpCode(501, "Not Implemented");

        List<SpanHttpDeriverUtil.HttpCode> codeList = Arrays.asList(code200, code400, code400, code302, code501);

        List<SpanHttpDeriverUtil.HttpCode> clientOrServerErrors = SpanHttpDeriverUtil.getClientOrServerErrors(codeList);
        Assert.assertEquals(
                Arrays.asList(new SpanHttpDeriverUtil.HttpCode(400, "400"),
                    new SpanHttpDeriverUtil.HttpCode(400, "400"),
                    new SpanHttpDeriverUtil.HttpCode(501, "501")),
                clientOrServerErrors);
        Assert.assertEquals(Arrays.asList(code400, code400, code501), clientOrServerErrors);
    }

    public BinaryAnnotation createHttpCodeBinaryAnnotation(String code) {
        BinaryAnnotation httpCodeBinaryAnnotation = new BinaryAnnotation();
        httpCodeBinaryAnnotation.setKey(Constants.ZIPKIN_BIN_ANNOTATION_HTTP_STATUS_CODE);
        httpCodeBinaryAnnotation.setValue(code);
        return httpCodeBinaryAnnotation;
    }
}
