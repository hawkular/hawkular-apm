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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hawkular.apm.server.api.model.zipkin.BinaryAnnotation;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class HttpCodesUtilTest {

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
                Arrays.asList(new HttpCodesUtil.HttpCode(200, "ok"),
                    new HttpCodesUtil.HttpCode(302, "302"),
                    new HttpCodesUtil.HttpCode(400, "400"),
                    new HttpCodesUtil.HttpCode(500, "500")),
                HttpCodesUtil .getHttpStatusCodes(binaryAnnotationList));

        binaryAnnotationList = Arrays.asList(other, other);
        Assert.assertEquals(Collections.emptyList(), HttpCodesUtil.getHttpStatusCodes(binaryAnnotationList));

        Assert.assertEquals("OK", HttpCodesUtil.getHttpStatusCodes(
                Arrays.asList(createHttpCodeBinaryAnnotation("200"))).get(0).getDescription());
    }

    @Test
    public void testGetHttpStatusCodesOfNullAndEmptyInput() {
        Assert.assertTrue(HttpCodesUtil.getHttpStatusCodes(null).isEmpty());
        Assert.assertTrue(HttpCodesUtil.getHttpStatusCodes(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testGetErrors() {
        HttpCodesUtil.HttpCode code200 = new HttpCodesUtil.HttpCode(200, "Ok");
        HttpCodesUtil.HttpCode code302 = new HttpCodesUtil.HttpCode(302, "Found");
        HttpCodesUtil.HttpCode code400 = new HttpCodesUtil.HttpCode(400, "Bad Request");
        HttpCodesUtil.HttpCode code501 = new HttpCodesUtil.HttpCode(501, "Not Implemented");

        List<HttpCodesUtil.HttpCode> codeList = Arrays.asList(code200, code400, code400, code302, code501);

        List<HttpCodesUtil.HttpCode> clientOrServerErrors = HttpCodesUtil.getClientOrServerErrors(codeList);
        Assert.assertEquals(
                Arrays.asList(new HttpCodesUtil.HttpCode(400, "400"),
                    new HttpCodesUtil.HttpCode(400, "400"),
                    new HttpCodesUtil.HttpCode(501, "501")),
                clientOrServerErrors);
        Assert.assertEquals(Arrays.asList(code400, code400, code501), clientOrServerErrors);
    }

    public BinaryAnnotation createHttpCodeBinaryAnnotation(String code) {
        BinaryAnnotation httpCodeBinaryAnnotation = new BinaryAnnotation();
        httpCodeBinaryAnnotation.setKey(HttpCodesUtil.ZIPKIN_HTTP_CODE_KEY);
        httpCodeBinaryAnnotation.setValue(code);
        return httpCodeBinaryAnnotation;
    }
}
