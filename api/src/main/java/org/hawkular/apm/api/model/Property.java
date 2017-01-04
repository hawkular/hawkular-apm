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
package org.hawkular.apm.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This class represents a property.
 *
 * @author gbrown
 */
public class Property implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonInclude(Include.NON_NULL)
    private String name;

    @JsonInclude(Include.NON_NULL)
    private String value;

    @JsonInclude(Include.NON_DEFAULT)
    private PropertyType type = PropertyType.Text;

    @JsonInclude(Include.NON_NULL)
    private Double number;

    /**
     * The default constructor.
     */
    public Property() {
    }

    /**
     * The constructor.
     *
     * @param name The name
     * @param value The value
     */
    public Property(String name, Object value) {
        this.name = name;
        this.value = (value == null ? null : value.toString());
        this.type = PropertyType.of(value);
    }

    /**
     * The constructor.
     *
     * @param name The name
     * @param value The value
     * @param type The type
     */
    public Property(String name, String value, PropertyType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the type
     */
    public PropertyType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(PropertyType type) {
        this.type = type;
    }

    /**
     * @return the number
     */
    public Double getNumber() {
        if (number == null && value != null && type == PropertyType.Number) {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(Double number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "Property [name=" + name + ", value=" + value + ", type=" + type + ", number=" + number + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Property other = (Property) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
