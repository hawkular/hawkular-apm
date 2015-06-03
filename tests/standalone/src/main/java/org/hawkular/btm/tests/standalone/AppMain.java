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
package org.hawkular.btm.tests.standalone;

/**
 * This class represents a test (standalone) application that will be instrumented.
 *
 * @author gbrown
 */
public class AppMain implements Runnable {

    /**
     * Main for the test app.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        System.out.println("************ TEST APP CALLED WITH "+args.length+" ARGUMENTS");

        AppMain main=new AppMain();

        Thread t=new Thread(main);

        t.start();
    }

    public void run() {
        System.out.println("************ TEST APP THREAD STARTED");
    }
}
