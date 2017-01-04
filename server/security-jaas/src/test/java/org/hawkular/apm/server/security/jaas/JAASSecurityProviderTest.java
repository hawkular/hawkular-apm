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
package org.hawkular.apm.server.security.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.hawkular.apm.server.api.security.SecurityProviderException;
import org.junit.Test;

/**
 * @author gbrown
 */
public class JAASSecurityProviderTest {

    @Test
    public void testValidateDefault() {
        JAASSecurityProvider sp = new JAASSecurityProvider();

        String result = null;
        try {
            result = sp.validate(null, "anyone");
        } catch (SecurityProviderException e) {
            fail("SecurityProviderException thrown: "+e);
        }

        assertEquals(JAASSecurityProvider.DEFAULT_TENANT, result);
    }

}
