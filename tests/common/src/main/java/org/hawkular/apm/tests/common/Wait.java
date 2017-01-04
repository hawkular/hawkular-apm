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
package org.hawkular.apm.tests.common;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;

/**
 * Utility class, providing methods for tests to wait for conditions before proceeding.
 * @author jpkroehling
 */
public class Wait {

    /**
     * Blocks until the given condition evaluates to true. The condition is evaluated every 50
     * milliseconds, so, the given condition should be an idempotent operation.
     * If the condition is not met within 10 seconds, an exception is thrown.
     *
     * @param condition the condition to wait for
     */
    public static void until(Callable<Boolean> condition) {
        until(condition, 10, TimeUnit.SECONDS);
    }

    /**
     * Blocks until the given condition evaluates to true. The condition is evaluated every 50
     * milliseconds, so, the given condition should be an idempotent operation.
     * If the condition is not met within the given timeout, an exception is thrown.
     *
     * @param condition the condition to wait for
     * @param timeout the timeout value
     * @param timeUnit the unit for the timeout
     */
    public static void until(Callable<Boolean> condition, long timeout, TimeUnit timeUnit) {
        until(condition, timeout, timeUnit, 50);
    }

    /**
     * Blocks until the given condition evaluates to true. The condition is evaluated every @code{frequency}
     * milliseconds, so, the given condition should be an idempotent operation.
     * If the condition is not met within the given timeout, an exception is thrown.
     *
     * @param condition the condition to wait for
     * @param timeout the timeout value
     * @param timeUnit the unit for the timeout
     * @param frequency the frequency of the condition's evaluation in milliseconds
     */
    public static void until(Callable<Boolean> condition, long timeout, TimeUnit timeUnit, long frequency) {
        FutureTask<Void> futureTask = new FutureTask<Void>(() -> {
            while (!condition.call()) {
                Thread.sleep(frequency);
            }
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(futureTask);
        try {
            futureTask.get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            futureTask.cancel(true);
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Waits for a given port to become used.
     *
     * @param port
     */
    public static void forPortToBeUsed(int port) {
        Wait.until(portChecker(false, port));
    }

    /**
     * Waits for a given port to be free
     *
     * @param port
     */
    public static void forPortToBeFree(int port) {
        Wait.until(portChecker(true, port));
    }

    private static Callable<Boolean> portChecker(boolean freeCheck, int port) {
        return () -> {
            Socket socket = new Socket();
            socket.setSoTimeout(500);
            try {
                System.out.println("Trying to connect to port " + port);
                socket.connect(new InetSocketAddress(port), 500);
                socket.close();
                System.out.println("Was able to connect to port " + port);
                return !freeCheck;
            } catch (SocketTimeoutException | ConnectException ignored) {
                System.out.println("Was NOT able to connect to port " + port);
                return freeCheck;
            }
        };
    }
}
