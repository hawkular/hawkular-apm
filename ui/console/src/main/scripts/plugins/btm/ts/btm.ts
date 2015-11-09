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

  export var BTMController = _module.controller("BTM.BTMController", ["$scope", "$http", '$location', '$interval', ($scope, $http, $location, $interval) => {

    $scope.newBTxnName = '';
    $scope.candidateCount = 0;

    $scope.reload = function() {
      $http.get('/hawkular/btm/config/businesstxnsummary').then(function(resp) {
        $scope.businessTransactions = [];
        for (var i = 0; i < resp.data.length; i++) {
          var btxn = {
            summary: resp.data[i],
            count: undefined,
            faultcount: undefined,
            percentile95: undefined,
            alerts: undefined
          };
          $scope.businessTransactions.add(btxn);

          $scope.getBusinessTxnDetails(btxn);
        }
      },function(resp) {
        console.log("Failed to get business txn summaries: "+resp);
      });

      $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function(resp) {
        $scope.candidateCount = Object.keys(resp.data).length;
      },function(resp) {
        console.log("Failed to get candidate count: "+resp);
      });
    };

    $scope.reload();

    $interval(function() {
      $scope.reload();
    },10000);

    $scope.getBusinessTxnDetails = function(btxn) {
      $http.get('/hawkular/btm/analytics/businesstxn/completion/count?name='+btxn.summary.name).then(function(resp) {
        btxn.count = resp.data;
      },function(resp) {
        console.log("Failed to get count: "+resp);
      });

      $http.get('/hawkular/btm/analytics/businesstxn/completion/percentiles?name='+btxn.summary.name).then(function(resp) {
        if (resp.data.percentiles[95] > 0) {
          btxn.percentile95 = Math.round( resp.data.percentiles[95] / 1000000 ) / 1000;
        } else {
          btxn.percentile95 = 0;
        }
      },function(resp) {
        console.log("Failed to get completion percentiles: "+resp);
      });

      $http.get('/hawkular/btm/analytics/businesstxn/completion/faultcount?name='+btxn.summary.name).then(function(resp) {
        btxn.faultcount = resp.data;
      },function(resp) {
        console.log("Failed to get fault count: "+resp);
      });

      $http.get('/hawkular/btm/analytics/alerts/count/'+btxn.summary.name).then(function(resp) {
        btxn.alerts = resp.data;
      },function(resp) {
        console.log("Failed to get alerts count: "+resp);
      });
    };

    $scope.deleteBusinessTxn = function(btxn) {
      if (confirm('Are you sure you want to delete business transaction \"'+btxn.summary.name+'\"?')) {
        $http.delete('/hawkular/btm/config/businesstxn/'+btxn.summary.name).then(function(resp) {
          console.log('Deleted: '+btxn.summary.name);
          $scope.businessTransactions.remove(btxn);
        },function(resp) {
          console.log("Failed to delete business txn '"+btxn.summary.name+"': "+resp);
        });
      }
    };

  }]);

}
