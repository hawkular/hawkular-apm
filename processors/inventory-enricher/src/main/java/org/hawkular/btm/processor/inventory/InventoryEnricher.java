/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.btm.processor.inventory;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.hawkular.btm.api.model.btxn.BusinessTransaction;
import org.hawkular.btm.processor.inventory.log.MsgLogger;
import org.hawkular.btm.server.api.processors.BusinessTransactionFragmentHandler;
import org.jboss.logging.Logger;

/**
 * This class represents the inventory enricher, responsible for analysing reported
 * business transaction fragments to determine if new inventory resources/details
 * can be provided.
 *
 * @author gbrown
 */
public class InventoryEnricher implements BusinessTransactionFragmentHandler {

    private final Logger log = Logger.getLogger(InventoryEnricher.class);

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    @Inject
    private Instance<InventoryService> injectedInventoryService;

    private InventoryService inventoryService;

    @PostConstruct
    public void init() {
        if (injectedInventoryService.isUnsatisfied()) {
            msgLog.warnNoInventoryService();
        } else {
            inventoryService = injectedInventoryService.get();
        }
    }

    /**
     * @return the inventoryService
     */
    public InventoryService getInventoryService() {
        return inventoryService;
    }

    /**
     * @param inventoryService the inventoryService to set
     */
    public void setInventoryService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /* (non-Javadoc)
     * @see org.hawkular.btm.api.processors.BusinessTransactionFragmentHandler#handle(java.lang.String,java.util.List)
     */
    @Override
    public void handle(String tenantId, List<BusinessTransaction> btxns) {
        log.tracef("Inventory Enricher called with: %s", btxns);
    }

}
