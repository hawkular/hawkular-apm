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

package org.hawkular.apm.server.api.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A generic observable property.
 *
 * @author Pavol Loffay
 */
public class ObservableProperty<T> implements Serializable {

    public interface Observer {
        void update();
    }

    private String id;
    private T value;
    private List<Observer> observers = new CopyOnWriteArrayList<>();


    public ObservableProperty(String id, T value) {
        this.id = id;
        this.value = value;
    }

    public ObservableProperty(String id, T value, Collection<Observer> observers) {
        this.id = id;
        this.value = value;
        this.observers.addAll(observers);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void addAllObservers(Collection<Observer> observers) {
        observers.addAll(observers);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void updateValue(T value) {
        this.value = value;
        observers.forEach(Observer::update);
    }

    public T getValue() {
        return value;
    }

    public String getId() {
        return id;
    }

    public List<Observer> getObservers() {
        return observers;
    }
}
