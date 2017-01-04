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
package org.hawkular.apm.performance.server;

/**
 * @author gbrown
 */
public class Metrics {

    private String name;

    private int serviceCount = 0;
    private int maxServiceCount = 0;
    private int minServiceCount = 0;

    private long publishTracesDuration = 0;
    private int publishTracesCount = 0;

    public Metrics(String name) {
        this.setName(name);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public synchronized void createService(String name) {
        serviceCount++;
        if (serviceCount > maxServiceCount) {
            maxServiceCount = serviceCount;
        }
    }

    public synchronized void closeService(String name) {
        serviceCount--;
        if (serviceCount < minServiceCount) {
            minServiceCount = serviceCount;
        }
    }

    public synchronized void publishTraces(long duration) {
        publishTracesDuration += duration;
        publishTracesCount++;
    }

    public synchronized void report() {
        long avg = 0;
        if (publishTracesCount > 0) {
            avg = (publishTracesDuration / publishTracesCount);
        }
        System.out.println("[" + name + "] service max=" + maxServiceCount + " min=" + minServiceCount +
                " publish avg=" + avg + " count=" + publishTracesCount);

        minServiceCount = maxServiceCount = serviceCount;
        publishTracesCount = 0;
        publishTracesDuration = 0;
    }
}
