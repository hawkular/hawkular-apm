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
package org.hawkular.btm.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Juraci Paixão Kröhling
 */
@WebServlet("/keycloak.json")
public class KeycloakConfigurationServlet extends HttpServlet {
    private static final Pattern PROPERTY_REPLACEMENT_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        PrintWriter writer = resp.getWriter();
        writer.println(getKeycloakJson());
        writer.close();
    }

    private String getKeycloakJson() throws IOException {
        String rawKeycloakJson = getRawKeycloakJson();
        Matcher matcher = PROPERTY_REPLACEMENT_PATTERN.matcher(rawKeycloakJson);
        while(matcher.find()) {
            String propertyNotation = matcher.group();
            String propertyName = matcher.group(1);
            String value = System.getProperty(propertyName);
            if (null == value || value.isEmpty()) {
                value = propertyNotation;
            }
            rawKeycloakJson = rawKeycloakJson.replaceFirst(
                    Pattern.quote(propertyNotation),
                    Matcher.quoteReplacement(value)
            );
        }

        return rawKeycloakJson;
    }

    private String getRawKeycloakJson() throws IOException {
        InputStream keycloakJson = getClass().getResourceAsStream("/keycloak.json");
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(keycloakJson))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}
