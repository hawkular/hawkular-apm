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
package org.hawkular.btm.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.hawkular.btm.api.model.btxn.BusinessTransactionList;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;


/**
 * REST interface for reporting and querying business transactions.
 *
 * @author gbrown
 *
 */
@Path("/")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/", description = "Report & Query Business Transactions")
public class BusinessTransactionHandler {

    @POST
    @Path("/transactions")
    @ApiOperation(value = "Add a list of business transactions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Adding business transactions succeeded."),
            @ApiResponse(code = 500, message = "Unexpected error happened while storing the business transactions",
                response = ApiError.class) })
    public void addBusinessTransactions(@Suspended final AsyncResponse asyncResponse,
            @ApiParam(value = "List of business transactions", required = true) BusinessTransactionList btxns) {
    }

}
