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

  export var BTMController = _module.controller("BTM.BTMController", ["$scope", "$http", '$location', ($scope, $http, $location) => {

    $scope.newBTxnName = '';

    $http.get('/hawkular/btm/config/businesstxnnames').success(function(data) {
      $scope.businessTransactions = [];
      for (var i = 0; i < data.length; i++) {
        var btxn = {
          name: data[i],
          count: undefined,
          faultcount: undefined,
          percentile95: undefined,
          alerts: undefined
        };
        $scope.businessTransactions.add(btxn);

        $http.get('/hawkular/btm/analytics/businesstxn/count?name='+btxn.name).success(function(data) {
          btxn.count = data;
        });

        $http.get('/hawkular/btm/analytics/businesstxn/stats?name='+btxn.name).success(function(data) {
          $scope.stats = data;
          if (data.percentiles[95] > 0) {
            btxn.percentile95 = Math.round( data.percentiles[95] / 1000000 ) / 1000;
          }
        });

        $http.get('/hawkular/btm/analytics/businesstxn/faultcount?name='+btxn.name).success(function(data) {
          btxn.faultcount = data;
        });

        $http.get('/hawkular/btm/analytics/alerts/count/'+btxn.name).success(function(data) {
          btxn.alerts = data;
        });
      }
    });

    $scope.addBusinessTxn = function() {
      var defn = { };
      console.log('Add: '+$scope.newBTxnName);
      $http.put('/hawkular/btm/config/businesstxn/'+$scope.newBTxnName, defn).success(function(data) {
        $location.path('config/'+$scope.newBTxnName);
      });
    };

    $scope.deleteBusinessTxn = function(btxn) {
      if (confirm('Are you sure you want to delete business transaction \"'+btxn.name+'\"?')) {
        $http.delete('/hawkular/btm/config/businesstxn/'+btxn.name).success(function(data) {
          console.log('Deleted: '+btxn.name);
          $scope.businessTransactions.remove(btxn);
        });
      }
    };

  }]);

}
