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

package org.hawkular.apm.tests.dockerized.model;

import java.util.List;

/**
 * Class holding test case verify strategies. In general test case can be verified by json path, script, java code.
 * Currently on json path is implemented.
 *
 * @author Pavol Loffay
 */
public class TestCaseVerify {

    private List<JsonPathVerify> jsonPath;

    public List<JsonPathVerify> getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(List<JsonPathVerify> jsonPath) {
        this.jsonPath = jsonPath;
    }

    @Override
    public String toString() {
        return "TestCaseVerify{" +
                "jsonPath=" + jsonPath +
                '}';
    }
}
