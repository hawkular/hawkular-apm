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
package org.hawkular.btm.client.collector.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.btm.api.client.Logger;
import org.hawkular.btm.api.client.Logger.Level;

/**
 * This class manages the set of business fragment builders.
 *
 * @author gbrown
 */
public class FragmentManager {

    private static final Logger log=Logger.getLogger(FragmentManager.class.getName());

    private ThreadLocal<FragmentBuilder> builders = new ThreadLocal<FragmentBuilder>();

    private AtomicInteger threadCounter=new AtomicInteger();

    /**
     * @return the threadCounter
     */
    protected int getThreadCounter() {
        return threadCounter.get();
    }

    /**
     * This method returns whether the current thread has a fragment builder.
     *
     * @return Whether the current thread of execution has a fragment builder
     */
    public boolean hasFragmentBuilder() {
        return builders.get() != null;
    }

    /**
     * This method returns the appropriate fragment builder for the current
     * thread.
     *
     * @return The fragment builder for this thread of execution
     */
    public FragmentBuilder getFragmentBuilder() {
        FragmentBuilder builder = builders.get();

        if (builder == null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Creating new FragmentBuilder");
            }
            builder = new FragmentBuilder();
            builders.set(builder);

            int currentCount=threadCounter.incrementAndGet();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Associate Thread with FragmentBuilder(1): current thread count="+currentCount);
            }
        }

        return builder;
    }

    /**
     * This method sets the builder for this thread of execution.
     *
     * @param builder The fragment builder
     */
    public void setFragmentBuilder(FragmentBuilder builder) {
        FragmentBuilder currentBuilder=builders.get();

        if (currentBuilder == null && builder != null) {
            int currentCount=threadCounter.incrementAndGet();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Associate Thread with FragmentBuilder(2): current thread count="+currentCount);
            }
        } else if (currentBuilder != null && builder == null) {
            int currentCount=threadCounter.decrementAndGet();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Disassociate Thread from FragmentBuilder(2): current thread count="+currentCount);
            }
        }

        builders.set(builder);
    }

    /**
     * This method clears the business transaction fragment builder for the
     * current thread of execution.
     */
    public void clear() {
        int currentCount=threadCounter.decrementAndGet();
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Clear: Disassociate Thread from FragmentBuilder(1): current thread count="+currentCount);
        }
        builders.remove();
    }
}
