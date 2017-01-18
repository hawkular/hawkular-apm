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
package org.hawkular.apm.client.opentracing;

import org.hawkular.apm.api.utils.PropertyUtil;

/**
 * @author Juraci Paixão Kröhling
 */
public class DeploymentMetaData {
    private static String ENV_SERVICE_NAME = getServiceNameFromEnv();
    private static String ENV_BUILD_STAMP = getBuildStampFromEnv();
    private static DeploymentMetaData INSTANCE = new DeploymentMetaData(ENV_SERVICE_NAME, ENV_BUILD_STAMP);

    private String serviceName;
    private String buildStamp;

    public DeploymentMetaData(String serviceName, String buildStamp) {
        this.serviceName = serviceName;
        this.buildStamp = buildStamp;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getBuildStamp() {
        return buildStamp;
    }

    public void setBuildStamp(String buildStamp) {
        this.buildStamp = buildStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeploymentMetaData that = (DeploymentMetaData) o;

        if (!serviceName.equals(that.serviceName)) return false;
        return buildStamp.equals(that.buildStamp);

    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + buildStamp.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DeploymentMetaData{" +
                "serviceName='" + serviceName + '\'' +
                ", buildStamp='" + buildStamp + '\'' +
                '}';
    }

    static String getBuildStampFromEnv() {
        String buildStamp = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_BUILDSTAMP);
        if (null != buildStamp && !buildStamp.isEmpty()) {
            return buildStamp;
        }
        String buildName = PropertyUtil.getProperty(PropertyUtil.OPENSHIFT_BUILD_NAME);
        if (null != buildName && !buildName.isEmpty()) {
            // it seems we are inside an openshift environment, as we do have the OPENSHIFT_BUILD_NAME env var set!

            String buildNamespace = PropertyUtil.getProperty(PropertyUtil.OPENSHIFT_BUILD_NAMESPACE);
            if (null == buildNamespace || buildNamespace.isEmpty()) {
                // that's odd: we have the value for OPENSHIFT_BUILD_NAME, but not for OPENSHIFT_BUILD_NAMESPACE
                // let's just ignore the namespace
                return buildName;
            } else {
                return String.format("%s.%s", buildNamespace, buildName);
            }
        }
        return null;
    }

    /**
     * The Service Name for this deployment, based on either an explicit service name property, or based
     * on environmental data, such as OpenShift build name/namespace.
     *
     * @return the service name to include on the trace context
     */
    static String getServiceNameFromEnv() {
        String serviceName = PropertyUtil.getProperty(PropertyUtil.HAWKULAR_APM_SERVICE_NAME);
        if (null == serviceName || serviceName.isEmpty()) {
            String buildStamp = getBuildStampFromEnv();
            if (null != buildStamp && !buildStamp.isEmpty()) {
                return getServiceFromBuildName(buildStamp);
            }
            return null;
        }
        return serviceName;
    }

    /**
     * Converts an OpenShift build name into a service name
     * @param buildName the build name, such as "hawkular-apm-1"
     * @return the service name, such as "hawkular-apm"
     */
    static String getServiceFromBuildName(String buildName) {
        if (null == buildName || buildName.isEmpty()) {
            return buildName;
        }

        return buildName.substring(0, buildName.lastIndexOf('-'));
    }

    static void reloadServiceName() {
        ENV_SERVICE_NAME = getServiceNameFromEnv();
        ENV_BUILD_STAMP = getBuildStampFromEnv();
        INSTANCE = new DeploymentMetaData(ENV_SERVICE_NAME, ENV_BUILD_STAMP);
    }

    static DeploymentMetaData getInstance() {
        return INSTANCE;
    }
}
