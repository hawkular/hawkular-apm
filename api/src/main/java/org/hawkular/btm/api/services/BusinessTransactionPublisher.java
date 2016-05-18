/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.api.services;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;

/**
 * This interface provides the capability for publishing business transaction
 * fragments.
 *
 * @author gbrown
 */
public interface BusinessTransactionPublisher extends Publisher<BusinessTransaction> {

    /**
     * This method indicates whether this publisher is enabled.
     *
     * @return Whether the publisher is enabled
     */
    boolean isEnabled();

}
