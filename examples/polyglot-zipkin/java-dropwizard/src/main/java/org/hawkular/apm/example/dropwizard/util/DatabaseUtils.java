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

package org.hawkular.apm.example.dropwizard.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Pavol Loffay
 */
public class DatabaseUtils {

    private DatabaseUtils() {}

    /**
     * Executes database script from resources directory
     */
    public static void executeDatabaseScript(String script) throws SQLException, MalformedURLException {
        File file = new File(script);
        DatabaseUtils.executeSqlScript(DatabaseUtils.getDBConnection(null), file.toURI().toURL());
    }

    /**
     * Executes SQL script.
     */
    private static void executeSqlScript(Connection connection, URL scriptUrl) throws SQLException {
        for (String sqlStatement : readSqlStatements(scriptUrl)) {
            if (!sqlStatement.trim().isEmpty()) {
                connection.prepareStatement(sqlStatement).executeUpdate();
            }
        }
    }

    public static Connection getDBConnection(String database) {
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(connectionString(database));
        } catch (ClassNotFoundException | SQLException ex)  {
            ex.printStackTrace();
        }

        return connection;
    }

    /**
     * Reads SQL statements from file. SQL commands in file must be separated by
     * a semicolon.
     *
     * @param url url of the file
     * @return array of command  strings
     */
    private static String[] readSqlStatements(URL url) {
        try {
            char buffer[] = new char[256];
            StringBuilder result = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(url.openStream(), "UTF-8");
            while (true) {
                int count = reader.read(buffer);
                if (count < 0) {
                    break;
                }
                result.append(buffer, 0, count);
            }
            return result.toString().split(";");
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read " + url, ex);
        }
    }

    private static String connectionString(String database) {
        StringBuilder connectionStr = new StringBuilder("jdbc:mysql://localhost");
        if (database != null) {
            connectionStr.append("/").append(database);
        }
        connectionStr.append("?statementInterceptors=com.github.kristofa.brave.mysql.MySQLStatementInterceptor");
        return connectionStr.toString();
    }
}
