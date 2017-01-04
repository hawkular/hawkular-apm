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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.api.services.PublisherMetricHandler;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.api.services.TracePublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author gbrown
 */
public class ClientSimulator {

    private static final Logger log = Logger.getLogger(ClientSimulator.class.getName());

    private SystemConfiguration systemConfig;

    private int invocations;

    private int requesters;

    private String name;

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: APMClientSimulator configFile invocations requesters name");
            System.err.println("    configFile - json format description of services");
            System.err.println("    invocations - number of invocations per requester");
            System.err.println("    requesters - number of concurrent requesters");
            System.err.println("    name - the simulator name, used to name the report file");
            System.exit(1);
        }

        ClientSimulator cs = null;

        try {
            cs = new ClientSimulator(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]);
            cs.run();
        } catch (Exception e1) {
            System.err.println("Error: " + e1);
        }
    }

    public ClientSimulator(String configFile, int invocations, int requesters, String name) throws Exception {
        init(configFile);
        this.invocations = invocations;
        this.requesters = requesters;
        this.name = name;
    }

    protected void init(String configFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        systemConfig = mapper.readValue(new File(configFile), SystemConfiguration.class);
    }

    public void run() {
        Metrics metrics = new Metrics(name);
        ServiceRegistry reg = new DefaultServiceRegistry(systemConfig, metrics);

        List<PathConfiguration> paths = new ArrayList<PathConfiguration>();
        for (PathConfiguration pc : systemConfig.getPaths()) {
            for (int i = 0; i < pc.getWeight(); i++) {
                paths.add(pc);
            }
        }

        // Initialise publisher metrics handler
        TracePublisher publisher = ServiceResolver.getSingletonService(TracePublisher.class);
        if (publisher == null) {
            log.severe("Trace publisher has not been configured correctly");
            return;
        }

        publisher.setMetricHandler(new PublisherMetricHandler<Trace>() {
            @Override
            public void published(String tenantId, List<Trace> items, long metric) {
                metrics.publishTraces(metric);
            }
        });

        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        }).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                metrics.report();
            }
        }, 1, 1, TimeUnit.SECONDS);

        for (int i = 0; i < requesters; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("THREAD: " + Thread.currentThread() + ": STARTED");

                    for (int j = 0; j < invocations; j++) {
                        // Randomly select path
                        int index = (int) (Math.random() * (paths.size() - 1));

                        Service s = reg.getServiceInstance(paths.get(index).getService());

                        Message m = new Message(paths.get(index).getName());

                        s.call(m, null, null);
                    }

                    System.out.println("THREAD: " + Thread.currentThread() + ": FINISHED: " + new java.util.Date());

                    synchronized (this) {
                        try {
                            wait(2000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
