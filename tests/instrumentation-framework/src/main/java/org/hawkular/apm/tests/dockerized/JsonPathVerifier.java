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

package org.hawkular.apm.tests.dockerized;

import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;

import com.jayway.jsonpath.JsonPath;

/**
 * This class verifies json path expression against give json.
 *
 * @author Pavol Loffay
 */
public class JsonPathVerifier {

    /**
     * Verifies that in the input json there is expected element defined as json path.
     *
     * @param json Json.
     * @param jsonPathVerify Json path expression and expected result.
     * @return
     */
    public static boolean verify(String json, JsonPathVerify jsonPathVerify) {

        Object pathResultObject = JsonPath.read(json, jsonPathVerify.getPath());

        return pathResultObject.equals(jsonPathVerify.getResult());
    }
}
