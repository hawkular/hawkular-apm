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
package org.hawkular.apm.tests.client.jav;

import java.util.logging.Logger;

/**
 * @author gbrown
 */
public class TopLevelService {

    private static final Logger log = Logger.getLogger(TopLevelService.class.getName());

    private InnerService innerService = new InnerService();

    /**
     * This is a test method.
     *
     * @param p1
     * @param p2
     * @return
     */
    public String testOp(String p1, int p2) {
        log.info("testOp called with: " + p1 + " " + p2);
        return innerService.join(p1, p2);
    }
}
