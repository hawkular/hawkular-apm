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
package org.hawkular.apm.api.model.config.instrumentation.jvm;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.api.model.config.instrumentation.InstrumentRule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents an instrumentation rule.
 *
 * @author gbrown
 */
public class JVM extends InstrumentRule {

    @JsonInclude
    private String className;

    @JsonInclude
    private String interfaceName;

    @JsonInclude
    private String methodName;

    @JsonInclude
    private List<String> parameterTypes = new ArrayList<String>();

    @JsonInclude
    private String helper;

    @JsonInclude
    private String location;

    @JsonInclude
    private List<InstrumentBind> binds = new ArrayList<InstrumentBind>();

    @JsonInclude
    private String condition;

    @JsonInclude
    private List<InstrumentAction> actions = new ArrayList<InstrumentAction>();

    @JsonInclude(Include.NON_NULL)
    private String fromVersion;

    @JsonInclude(Include.NON_NULL)
    private String toVersion;

    @JsonInclude(Include.NON_DEFAULT)
    private boolean compile = true;

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the interfaceName
     */
    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * @param interfaceName the interfaceName to set
     */
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * @return the methodName
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param methodName the method to set
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return the parameterTypes
     */
    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    /**
     * @param parameterTypes the parameterTypes to set
     */
    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    /**
     * @return the helper
     */
    public String getHelper() {
        return helper;
    }

    /**
     * @param helper the helper to set
     */
    public void setHelper(String helper) {
        this.helper = helper;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the binds
     */
    public List<InstrumentBind> getBinds() {
        return binds;
    }

    /**
     * @param binds the binds to set
     */
    public void setBinds(List<InstrumentBind> binds) {
        this.binds = binds;
    }

    /**
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * @return the actions
     */
    public List<InstrumentAction> getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(List<InstrumentAction> actions) {
        this.actions = actions;
    }

    /**
     * @return the 'from' version
     */
    public String getFromVersion() {
        return fromVersion;
    }

    /**
     * @param version the 'from' version to set
     */
    public void setFromVersion(String version) {
        this.fromVersion = version;
    }

    /**
     * @return the 'to' version
     */
    public String getToVersion() {
        return toVersion;
    }

    /**
     * @param version the 'to' version to set
     */
    public void setToVersion(String version) {
        this.toVersion = version;
    }

    /**
     * This method determines if the rule is valid for the
     * supplied version. If the supplied version is null, then
     * it is assumed to represent 'latest version' and therefore
     * any rule with a 'toVersion' set will not be valid.
     *
     * @param version The version
     * @return Whether the rule is valid for the supplied version
     */
    public boolean isVersionValid(String version) {
        if (fromVersion != null && version != null) {
            if (version.compareTo(fromVersion) < 0) {
                return false;
            }
        }
        if (toVersion != null) {
            if (version == null || version.compareTo(toVersion) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the compile
     */
    public boolean isCompile() {
        return compile;
    }

    /**
     * @param compile the compile to set
     */
    public void setCompile(boolean compile) {
        this.compile = compile;
    }

}
