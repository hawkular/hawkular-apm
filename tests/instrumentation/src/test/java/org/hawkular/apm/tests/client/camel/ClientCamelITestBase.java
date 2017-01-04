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
package org.hawkular.apm.tests.client.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.hawkular.apm.tests.common.ClientTestBase;

/**
 * @author gbrown
 */
public abstract class ClientCamelITestBase extends ClientTestBase {

    private CamelContext context = new DefaultCamelContext();

    @Override
    public void init() {
        try {
            initContext(context);

            context.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.init();
    }

    /**
     * This method initialises the camel context.
     *
     * @param context The camel context
     * @throws Exception Failed to initialise
     */
    protected void initContext(CamelContext context) throws Exception {
        context.addRoutes(getRouteBuilder());
    }

    /**
     * This method defines the route to be tested.
     *
     * @return The route builder
     */
    protected abstract RouteBuilder getRouteBuilder();

    @Override
    public void close() {
        try {
            context.stop();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        super.close();
    }

}
