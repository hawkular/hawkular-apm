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
package org.hawkular.apm.server.api.security;

/**
 * This interface represents a security provider responsible for identifying the current tenant id.
 *
 * @author gbrown
 */
public interface SecurityProvider {

    /**
     * This method validates that the principal has access to the requested tenant.
     * If so, then the tenant will be returned, otherwise an exception will be
     * thrown.
     *
     * @param tenant The tenant
     * @param principal The principal
     * @return The tenant to be used
     * @throws SecurityProviderException Principal does not have access to the tenant
     */
    String validate(String tenant, String principal) throws SecurityProviderException;

}
