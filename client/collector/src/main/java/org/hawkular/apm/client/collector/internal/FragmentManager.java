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
package org.hawkular.apm.client.collector.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;

/**
 * This class manages the set of fragment builders.
 *
 * @author gbrown
 */
public class FragmentManager {

    private static final Logger log = Logger.getLogger(FragmentManager.class.getName());

    private ThreadLocal<FragmentBuilder> builders = new ThreadLocal<FragmentBuilder>();

    private AtomicInteger threadCounter = new AtomicInteger();
    private Set<String> threadNames = new HashSet<String>();

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

            int currentCount = threadCounter.incrementAndGet();
            int builderCount = builder.incrementThreadCount();

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Associate Thread with FragmentBuilder(1): Total Thread Count = " + currentCount
                        + " : Fragment Thread Count = " + builderCount);
                synchronized (threadNames) {
                    threadNames.add(Thread.currentThread().getName());
                }
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
        FragmentBuilder currentBuilder = builders.get();

        if (currentBuilder == null && builder != null) {
            int currentCount = threadCounter.incrementAndGet();
            int builderCount = builder.incrementThreadCount();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Associate Thread with FragmentBuilder(2): Total Thread Count = " + currentCount
                        + " : Fragment Thread Count = " + builderCount);
                synchronized (threadNames) {
                    threadNames.add(Thread.currentThread().getName());
                }
            }
        } else if (currentBuilder != null && builder == null) {
            int currentCount = threadCounter.decrementAndGet();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Disassociate Thread from FragmentBuilder(2): Total Thread Count = " + currentCount);
                synchronized (threadNames) {
                    threadNames.remove(Thread.currentThread().getName());
                }
            }
        } else if (currentBuilder != builder) {
            int oldCount = currentBuilder.decrementThreadCount();
            int newCount = builder.incrementThreadCount();
            if (log.isLoggable(Level.FINEST)) {
                log.finest("WARNING: Overwriting thread's fragment builder: old=[" + currentBuilder
                        + " count=" + oldCount + "] now=[" + builder + " count=" + newCount + "]");
            }
        }

        builders.set(builder);
    }

    /**
     * This method clears the trace fragment builder for the
     * current thread of execution.
     */
    public void clear() {
        int currentCount = threadCounter.decrementAndGet();
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Clear: Disassociate Thread from FragmentBuilder(1): current thread count=" + currentCount);
            synchronized (threadNames) {
                threadNames.remove(Thread.currentThread().getName());
            }
        }
        FragmentBuilder currentBuilder = builders.get();
        if (currentBuilder != null) {
            currentBuilder.decrementThreadCount();
        }
        builders.remove();
    }

    /**
     * This method reports diagnostic information to the log.
     */
    public void diagnostics() {
        log.finest("Thread count = " + threadCounter);
        if (threadCounter.get() > 0) {
            log.finest("Thread names:");
            synchronized (threadNames) {
                for (String name : threadNames) {
                    log.finest("\t" + name);
                }
            }
        }
    }
}
