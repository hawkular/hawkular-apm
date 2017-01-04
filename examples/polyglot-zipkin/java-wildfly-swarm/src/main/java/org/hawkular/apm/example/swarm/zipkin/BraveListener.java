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

package org.hawkular.apm.example.swarm.zipkin;


import java.util.EnumSet;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.servlet.BraveServletFilter;

@WebListener
public class BraveListener implements ServletContextListener {

    @Inject
    private Brave brave;

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        FilterRegistration.Dynamic servletFilter = contextEvent.getServletContext()
                .addFilter("BraveServletFilter", BraveServletFilter.create(brave));

        servletFilter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "*");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}
