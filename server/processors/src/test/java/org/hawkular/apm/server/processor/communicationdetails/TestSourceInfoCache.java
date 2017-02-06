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
package org.hawkular.apm.server.processor.communicationdetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.api.model.events.SourceInfo;
import org.hawkular.apm.server.api.services.SourceInfoCache;

/**
 * @author gbrown
 */
public class TestSourceInfoCache implements SourceInfoCache {

    private Map<String,SourceInfo> sourceInfoCache = new HashMap<String,SourceInfo>();

    @Override
    public SourceInfo get(String tenantId, String id) {
        return sourceInfoCache.get(id);
    }

    @Override
    public void store(String tenantId, List<SourceInfo> sourceInfoList) {
        for (int i=0; i < sourceInfoList.size(); i++) {
            SourceInfo si = sourceInfoList.get(i);
            sourceInfoCache.put(si.getId(), si);
        }
    }

}
