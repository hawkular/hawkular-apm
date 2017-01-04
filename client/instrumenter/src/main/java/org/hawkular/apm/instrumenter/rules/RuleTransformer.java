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
package org.hawkular.apm.instrumenter.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.config.instrumentation.InstrumentRule;
import org.hawkular.apm.api.model.config.instrumentation.Instrumentation;
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentAction;
import org.hawkular.apm.api.model.config.instrumentation.jvm.InstrumentBind;
import org.hawkular.apm.api.model.config.instrumentation.jvm.JVM;
import org.hawkular.apm.api.services.ServiceResolver;
import org.hawkular.apm.instrumenter.RuleHelper;

/**
 * @author gbrown
 */
public class RuleTransformer {

    private static Map<Class<? extends InstrumentAction>, InstrumentActionTransformer> transformers = new HashMap<Class<? extends InstrumentAction>, InstrumentActionTransformer>();

    static {
        List<InstrumentActionTransformer> trms = ServiceResolver.getServices(InstrumentActionTransformer.class);
        trms.forEach(t -> transformers.put(t.getActionType(), t));
    }

    /**
     * This method transforms the list of instrument types into a
     * ByteMan rule script.
     *
     * @param name The instrumentation rules name
     * @param types The instrument types
     * @param version The optional version
     * @return The rule script
     */
    public String transform(String name, Instrumentation types, String version) {
        StringBuilder builder = new StringBuilder();

        builder.append(types.isCompile() ? "COMPILE\r\n" : "NOCOMPILE\r\n");

        for (int ruleno = 0; ruleno < types.getRules().size(); ruleno++) {
            InstrumentRule r = types.getRules().get(ruleno);

            if (r.getClass() == JVM.class) {
                JVM rule = (JVM) r;

                // Check version
                if (!rule.isVersionValid(version)) {
                    continue;
                }

                builder.append("\r\n");

                builder.append("RULE ");
                builder.append(name);
                builder.append('(');
                builder.append(ruleno + 1);
                builder.append(") ");
                builder.append(rule.getRuleName());
                builder.append("\r\n");

                if (rule.getClassName() != null) {
                    builder.append("CLASS ");
                    builder.append(rule.getClassName());
                    builder.append("\r\n");
                } else if (rule.getInterfaceName() != null) {
                    builder.append("INTERFACE ");
                    builder.append(rule.getInterfaceName());
                    builder.append("\r\n");
                }

                builder.append("METHOD ");
                builder.append(rule.getMethodName());

                // Check if single parameter is "*", representing 'any' parameters
                if (rule.getParameterTypes().size() != 1 || !rule.getParameterTypes().get(0).equals("*")) {
                    builder.append('(');

                    for (int i = 0; i < rule.getParameterTypes().size(); i++) {
                        if (i > 0) {
                            builder.append(',');
                        }
                        builder.append(rule.getParameterTypes().get(i));
                    }

                    builder.append(')');
                }

                builder.append("\r\n");

                builder.append("HELPER ");
                if (rule.getHelper() == null) {
                    builder.append(RuleHelper.class.getName());
                } else {
                    builder.append(rule.getHelper());
                }
                builder.append("\r\n");

                builder.append("AT ");
                builder.append(rule.getLocation());
                builder.append("\r\n");

                if (rule.isCompile() != types.isCompile()) {
                    builder.append(rule.isCompile() ? "COMPILE\r\n" : "NOCOMPILE\r\n");
                }

                if (!rule.getBinds().isEmpty()) {
                    builder.append("BIND ");

                    for (int i = 0; i < rule.getBinds().size(); i++) {
                        InstrumentBind bind = rule.getBinds().get(i);
                        if (i > 0) {
                            builder.append("     ");
                        }
                        builder.append(bind.getName());
                        builder.append(" : ");
                        builder.append(bind.getType());
                        builder.append(" = ");
                        builder.append(bind.getExpression());
                        builder.append(";\r\n");
                    }
                }

                builder.append("IF ");
                if (rule.getCondition() == null) {
                    builder.append("TRUE");
                } else {
                    builder.append(rule.getCondition());
                }
                builder.append("\r\n");

                builder.append("DO\r\n");

                for (int i = 0; i < rule.getActions().size(); i++) {
                    InstrumentAction action = rule.getActions().get(i);

                    builder.append("  ");
                    InstrumentActionTransformer transformer = transformers.get(action.getClass());

                    if (transformer != null) {
                        builder.append(transformer.convertToRuleAction(rule.getActions().get(i)));
                        if (i < rule.getActions().size() - 1) {
                            builder.append(";");
                        }
                        builder.append("\r\n");
                    } else {
                        System.err.println("Transformer for action '" + action.getClass() + "' not found");
                    }
                }

                builder.append("ENDRULE\r\n\r\n");
            }
        }

        return builder.toString();
    }
}
