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
package org.hawkular.apm.processor.alerts;

import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

import org.hawkular.apm.api.utils.PropertyUtil;

import feign.Retryer;
import feign.auth.BasicAuthRequestInterceptor;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 * @author Juraci Paixão Kröhling
 */
@Stateless
public class AlertsPublisher {
    private static final MsgLogger logger = MsgLogger.LOGGER;
    private static final String BASE_URL = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_URI);
    private static final String USERNAME = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_USERNAME);
    private static final String PASSWORD = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_PASSWORD);
    private static final String TARGET = String.format("%s/hawkular/alerts", BASE_URL);

    @Asynchronous
    public void publish(List<Event> eventList) {
        eventList.forEach(completionTime -> publish(completionTime));
    }

    @Asynchronous
    public void publish(final Event event) {
        if (BASE_URL == null || BASE_URL.isEmpty()) {
            logger.hawkularServerNotConfigured();
            return;
        }

        if (USERNAME == null || USERNAME.isEmpty()) {
            logger.hawkularServerUsernameNotConfigured();
            return;
        }

        if (PASSWORD == null || PASSWORD.isEmpty()) {
            logger.hawkularServerPasswordNotConfigured();
            return;
        }

        HystrixFeign.builder()
                .requestInterceptor(new BasicAuthRequestInterceptor(USERNAME, PASSWORD))
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .retryer(new Retryer.Default())
                .target(AlertsService.class, TARGET)
                .addEvent(event);
    }
}
