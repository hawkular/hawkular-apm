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
package org.hawkular.apm.api.model.config;

import java.text.NumberFormat;
import java.text.ParseException;

/**
 * This enumerated type defines the reporting levels for trace
 * instance information.
 *
 * @author gbrown
 */
public enum ReportingLevel {

    /* The activity associated with this transaction is not of interest
     * (i.e. long term decision) */
    Ignore,

    /* Don't report any instances of this transaction, but unlike the
     * 'Ignore' level, this may only be a temporary situation, and the
     * trace is of interest */
    None,

    /* Report all information related to instances of this transaction */
    All;

    public static ReportingLevel parse(Object obj) {
        if (obj instanceof ReportingLevel) {
            return (ReportingLevel) obj;
        }

        if (obj instanceof String) {
            return ReportingLevel.valueOf((String)obj);
        } else if (!(obj instanceof Number)) {
            return null;
        }

        int priority;
        try {
            priority = NumberFormat.getInstance().parse(obj.toString()).intValue();
        } catch (ParseException e) {
            return null;
        }

        if (priority >= 1) {
            return ReportingLevel.All;
        }

        return ReportingLevel.None;
    }


}
