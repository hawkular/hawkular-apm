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
package org.hawkular.apm.api.internal.actions;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.apm.api.logging.Logger;
import org.hawkular.apm.api.logging.Logger.Level;
import org.hawkular.apm.api.model.config.txn.Expression;
import org.hawkular.apm.api.model.config.txn.JSONExpression;
import org.hawkular.apm.api.model.config.txn.LiteralExpression;
import org.hawkular.apm.api.model.config.txn.TextExpression;
import org.hawkular.apm.api.model.config.txn.XMLExpression;

/**
 * This class provides a factory for creating handlers associated with expressions.
 *
 * @author gbrown
 */
public class ExpressionHandlerFactory {

    private static final Logger log = Logger.getLogger(ExpressionHandlerFactory.class.getName());

    private static Map<Class<? extends Expression>, Class<? extends ExpressionHandler>> handlers;

    static {
        handlers = new HashMap<Class<? extends Expression>, Class<? extends ExpressionHandler>>();

        handlers.put(JSONExpression.class, JSONExpressionHandler.class);
        handlers.put(XMLExpression.class, XMLExpressionHandler.class);
        handlers.put(TextExpression.class, TextExpressionHandler.class);
        handlers.put(LiteralExpression.class, LiteralExpressionHandler.class);
    }

    /**
     * This method returns an expression handler for the supplied expression.
     *
     * @param expression The expression
     * @return The handler
     */
    public static ExpressionHandler getHandler(Expression expression) {
        ExpressionHandler ret = null;
        Class<? extends ExpressionHandler> cls = handlers.get(expression.getClass());
        if (cls != null) {
            try {
                Constructor<? extends ExpressionHandler> con = cls.getConstructor(Expression.class);
                ret = con.newInstance(expression);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Failed to instantiate handler for expression '" + expression + "'", e);
            }
        }
        return ret;
    }

}
