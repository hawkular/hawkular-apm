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

package org.hawkular.apm.tests.dockerized.exception;


import java.util.Collection;

import org.hawkular.apm.tests.dockerized.model.JsonPathVerify;
import org.hawkular.apm.tests.dockerized.model.TestCase;

/**
 * Exception which is thrown when test case fails.
 *
 * @author Pavol Loffay
 */
public class TestFailException extends Exception {

    private TestCase testCase;
    private Collection<JsonPathVerify> jsonPathVerify;

    public TestFailException() {
    }

    public TestFailException(String message) {
        super(message);
    }

    public TestFailException(TestCase testCase) {
        super(testCase.toString());
        this.testCase = testCase;
    }

    public TestFailException(TestCase testCase, Collection<JsonPathVerify> jsonPathVerify) {
        super(testCase.toString());
        this.testCase = testCase;
        this.jsonPathVerify = jsonPathVerify;
    }

    public TestFailException(TestCase testCase, String message) {
        super(message);
        this.testCase = testCase;
    }

    public TestFailException(TestCase testCase, Throwable cause) {
        super(testCase.toString(), cause);
        this.testCase = testCase;
    }

    public TestFailException(TestCase testCase, String message, Throwable cause) {
        super(message, cause);
        this.testCase = testCase;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public Collection<JsonPathVerify> getJsonPathVerify() {
        return jsonPathVerify;
    }

    @Override
    public String toString() {
        return "TestFailException{" +
                "testCase=" + testCase +
                ", jsonPathVerify=" + jsonPathVerify +
                '}';
    }
}
