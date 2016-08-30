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

package org.hawkular.apm.example.dropwizard.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hawkular.apm.example.dropwizard.model.User;
import org.hawkular.apm.example.dropwizard.util.DatabaseUtils;

/**
 * @author Pavol Loffay
 */
public class UserDAO {

    private static final String DATABASE = "DROPWIZARD";

    public User createUser(User user) {
        if(user == null){
            throw new IllegalArgumentException("parameter user is null");
        }
        try(Connection connection = DatabaseUtils.getDBConnection(DATABASE)){
            try(PreparedStatement st = connection.prepareStatement("INSERT INTO Users (name) values(?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)){
                st.setString(1, user.getName());
                st.executeUpdate();
                try(ResultSet keys = st.getGeneratedKeys()){
                    if(keys.next()){
                        long id = keys.getLong(1);
                        user.setId(id);
                    }
                }
            }
        } catch (SQLException ex){
            ex.printStackTrace();
            return null;
        }

        return user;
    }

    public User getUser(String id) {
        if(id == null){
            throw new IllegalArgumentException("parameter user is null");
        }

        User user = null;

        try(Connection connection = DatabaseUtils.getDBConnection(DATABASE)){
            try(PreparedStatement st = connection.prepareStatement("SELECT id, name FROM Users WHERE ID=?")){
                st.setString(1, id.toString());
                st.executeQuery();
                try(ResultSet keys = st.getResultSet()){
                    if(keys.next()){
                        user = new User(keys.getLong("id"), keys.getString("name"));
                    }
                }
            }
        } catch (SQLException ex){
            ex.printStackTrace();
            return null;
        }

        return user;
    }

    public Collection<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        try(Connection connection = DatabaseUtils.getDBConnection(DATABASE)){
            try(PreparedStatement st = connection.prepareStatement("SELECT id, name FROM Users")){
                try(ResultSet resultSet = st.executeQuery()) {
                    while (resultSet.next()) {
                        users.add(new User(resultSet.getLong("id"), resultSet.getString("name")));
                    }
                }
            }
        } catch (SQLException ex){
            ex.printStackTrace();
            return null;
        }

        return users;
    }
}
