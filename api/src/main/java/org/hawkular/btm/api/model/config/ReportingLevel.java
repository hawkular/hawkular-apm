/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.model.config;

/**
 * This enumerated type defines the reporting levels for business transaction
 * instance information.
 *
 * @author gbrown
 */
public enum ReportingLevel {

    /* The activity associated with this business txn is not of interest
     * (i.e. long term decision) */
    Ignore,

    /* Don't report any instances of this business txn, but unlike the
     * 'Ignore' level, this may only be a temporary situation, and the
     * business transaction is of interest */
    None,

    /* Report all information related to instances of this business txn */
    All

}
