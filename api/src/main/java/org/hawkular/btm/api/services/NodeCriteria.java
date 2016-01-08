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
package org.hawkular.btm.api.services;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.swagger.annotations.ApiModel;

/**
 * This class represents the query criteria for retrieving a set of node details.
 *
 * @author gbrown
 */
@ApiModel(parent = BaseCriteria.class)
public class NodeCriteria extends BaseCriteria {

    private final Logger log = Logger.getLogger(NodeCriteria.class.getName());

    /**
     * This method returns the criteria as a map of name/value pairs.
     *
     * @return The criteria parameters
     */
    @Override
    public Map<String, String> parameters() {
        Map<String, String> ret = super.parameters();

        if (log.isLoggable(Level.FINEST)) {
            log.finest("NodeCriteria parameters [" + ret + "]");
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NodeCriteria [toString()=" + super.toString() + "]";
    }

}
