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
package org.hawkular.btm.server.security.hwkaccs;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;

import org.hawkular.accounts.api.model.Persona;
import org.hawkular.btm.server.api.security.SecurityProvider;

/**
 * @author gbrown
 */
public class HawkularAccountsSecurityProvider implements SecurityProvider {

    @Inject
    Instance<Persona> currentPersona;

    /* (non-Javadoc)
     * @see org.hawkular.btm.server.api.services.SecurityProvider#getTenantId(javax.ws.rs.core.SecurityContext)
     */
    @Override
    public String getTenantId(SecurityContext context) {
        return currentPersona.get().getIdAsUUID().toString();
    }

}
