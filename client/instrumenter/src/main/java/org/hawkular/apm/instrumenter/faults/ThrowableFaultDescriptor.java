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
package org.hawkular.apm.instrumenter.faults;

/**
 * This class provides the fault descriptor for a Throwable fault.
 *
 * @author gbrown
 */
public class ThrowableFaultDescriptor implements FaultDescriptor {

    /* (non-Javadoc)
     * @see org.hawkular.apm.instrumenter.faults.FaultDescriptor#isValid(java.lang.Object)
     */
    @Override
    public boolean isValid(Object fault) {
        return fault instanceof Throwable;
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.instrumenter.faults.FaultDescriptor#getName(java.lang.Object)
     */
    @Override
    public String getName(Object fault) {
        return fault.getClass().getSimpleName();
    }

    /* (non-Javadoc)
     * @see org.hawkular.apm.instrumenter.faults.FaultDescriptor#getDescription(java.lang.Object)
     */
    @Override
    public String getDescription(Object fault) {
        return ((Throwable)fault).getMessage();
    }

}
