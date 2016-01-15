/// Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///   http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.

/// <reference path="btmPlugin.ts"/>
module BTM {

  export var BTMDisabledController = _module.controller("BTM.BTMDisabledController", ["$scope", "$http", '$location', '$interval', ($scope, $http, $location, $interval) => {

    $scope.newBTxnName = '';
    $scope.candidateCount = 0;

    $scope.reload = function() {
      $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
        $scope.businessTransactions = [];
        for (var i = 0; i < resp.data.length; i++) {
          var btxn = {
            summary: resp.data[i]
          };
          $scope.businessTransactions.add(btxn);
        }
      },function(resp) {
        console.log("Failed to get business txn summaries: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/unbounduris').then(function(resp) {
        $scope.candidateCount = Object.keys(resp.data).length;
      },function(resp) {
        console.log("Failed to get candidate count: "+JSON.stringify(resp));
      });
    };

    $scope.reload();

    $scope.deleteBusinessTxn = function(btxn) {
      if (confirm('Are you sure you want to delete business transaction \"'+btxn.summary.name+'\"?')) {
        $http.delete('/hawkular/btm/config/businesstxn/full/'+btxn.summary.name).then(function(resp) {
          console.log('Deleted: '+btxn.summary.name);
          $scope.businessTransactions.remove(btxn);
        },function(resp) {
          console.log("Failed to delete business txn '"+btxn.summary.name+"': "+JSON.stringify(resp));
        });
      }
    };

  }]);

}
