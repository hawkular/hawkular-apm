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

package org.hawkular.apm.tests.dockerized;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;

import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;

import com.jayway.jsonpath.JsonPath;

/**
 * This class verifies json path expression against give json.
 *
 * @author Pavol Loffay
 */
public class JsonPathVerifier {

    JsonPathVerifier() {}

    /**
     * Verifies that in the input json there is expected element defined as json path.
     *
     * @param json Json.
     * @param jsonPathVerify Json path expression and expected result.
     * @return
     */
    public static boolean verify(String json, JsonPathVerify jsonPathVerify) {

        Object leftObject = jsonPathVerify.getLeft();
        Object rightObject = jsonPathVerify.getRight();

        if (jsonPathVerify.getLeft().matches("\\$.*")) {
            leftObject= JsonPath.read(json, jsonPathVerify.getLeft());
        }
        if (jsonPathVerify.getRight().matches("\\$.*")) {
            rightObject = JsonPath.read(json, jsonPathVerify.getRight());
        }

        boolean result;
        switch (jsonPathVerify.getOperator()) {
            case EQ: result = leftObject.toString().equals(rightObject.toString());
                break;
            case NE: result = !leftObject.toString().equals(rightObject.toString());
                break;
            case LT:
                result = compareTo(leftObject.toString(), rightObject.toString()) < 0;
                break;
            case GT:
                result = compareTo(leftObject.toString(), rightObject.toString()) > 0;
                break;
            default:
                throw new IllegalStateException("Unsupported operator: " + jsonPathVerify.getOperator());
        }

        return result;
    }

    private static int compareTo(String n1, String n2) {
        BigDecimal b1 = BigDecimal.valueOf(parseNumber(n1).doubleValue());
        BigDecimal b2 = BigDecimal.valueOf(parseNumber(n2).doubleValue());
        return b1.compareTo(b2);
    }

    private static Number parseNumber(String str) {
        Number number;
        try {
            number = NumberFormat.getInstance().parse(str);
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse number: " + str, e);
        }
        return number;
    }
}
