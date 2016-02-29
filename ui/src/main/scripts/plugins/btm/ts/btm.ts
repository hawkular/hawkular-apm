///
/// Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
/// and other contributors as indicated by the @author tags.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///    http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/// <reference path="btmPlugin.ts"/>
module BTM {

  declare var c3: any;

  export var BTMController = _module.controller("BTM.BTMController", ["$scope", "$http", '$location', '$interval', '$q', '$timeout', ($scope, $http, $location, $interval, $q, $timeout) => {

    $scope.candidateCount = 0;

    $scope.businessTransactions = [];

    $scope.reload = function() {
      $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {

        var allPromises = [];
        for (var i = 0; i < resp.data.length; i++) {
          allPromises = allPromises.concat($scope.getBusinessTxnDetails(resp.data[i]));
        }

        $q.all(allPromises).then(() => {
          $scope.businessTransactions = resp.data;
          $scope.businessTransactions.$resolved = true;

          if (!$scope.btxncountpiechart || !$scope.btxnfaultcountpiechart) {
            $scope.initGraph();
          } else {
            $scope.reloadTxnCountGraph();
            $scope.reloadFaultCountGraph();
          }

        });
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

    var refreshPromise = $interval(() => { $scope.reload(); }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.getBusinessTxnDetails = function(btxn) {
      var promises = [];

      var countPromise = $http.get('/hawkular/btm/analytics/completion/count?businessTransaction='+btxn.name);
      promises.push(countPromise);
      countPromise.then(function(resp) {
        btxn.count = resp.data;
      }, function(resp) {
        console.log("Failed to get count: "+JSON.stringify(resp));
      });


      var pct95Promise = $http.get('/hawkular/btm/analytics/completion/percentiles?businessTransaction='+btxn.name);
      promises.push(pct95Promise);
      pct95Promise.then(function(resp) {
        if (resp.data.percentiles[95] > 0) {
          btxn.percentile95 = Math.round( resp.data.percentiles[95] / 1000000 ) / 1000;
        } else {
          btxn.percentile95 = 0;
        }
      },function(resp) {
        console.log("Failed to get completion percentiles: "+JSON.stringify(resp));
      });

      var faultsPromise = $http.get('/hawkular/btm/analytics/completion/faultcount?businessTransaction='+btxn.name);
      promises.push(faultsPromise);
      faultsPromise.then(function(resp) {
        btxn.faultcount = resp.data;
      },function(resp) {
        console.log("Failed to get fault count: "+JSON.stringify(resp));
      });

      var alertsPromise = $http.get('/hawkular/btm/analytics/alerts/count/'+btxn.name);
      promises.push(alertsPromise);
      alertsPromise.then(function(resp) {
        btxn.alerts = resp.data;
      },function(resp) {
        console.log("Failed to get alerts count: "+JSON.stringify(resp));
      });

      return promises;
    };

    $scope.deleteBusinessTxn = function(btxn) {
      if (confirm('Are you sure you want to delete business transaction \"'+btxn.name+'\"?')) {
        $http.delete('/hawkular/btm/config/businesstxn/full/'+btxn.name).then(function(resp) {
          console.log('Deleted: '+btxn.name);
          $scope.businessTransactions.remove(btxn);
        },function(resp) {
          console.log("Failed to delete business txn '"+btxn.name+"': "+JSON.stringify(resp));
        });
      }
    };

    $scope.initGraph = function() {
      $timeout(() => {
        $scope.btxncountpiechart = c3.generate({
          bindto: '#btxntxncountpiechart',
          data: {
            columns: [],
            type: 'pie',
            onclick: function(d, i) {
              $location.path('/hawkular-ui/btm/info/' + d.id);
            }
          }
        });

        $scope.btxnfaultcountpiechart = c3.generate({
          bindto: '#btxnfaultcountpiechart',
          data: {
            columns: [],
            type: 'pie',
            onclick: function(d, i) {
              $location.path('/hawkular-ui/btm/info/' + d.id);
            }
          }
        });

        $scope.reloadTxnCountGraph();
        $scope.reloadFaultCountGraph();
      });
    };

    $scope.reloadTxnCountGraph = function() {
      var btxnCountData = [];
      _.each($scope.businessTransactions, (btxn: any) => {
        if (btxn.count) {
          btxnCountData.push([btxn.name, btxn.count]);
        }
      });

      $scope.btxncountpiechart.load({
        columns: btxnCountData
      });

      $scope.countChartHasData = btxnCountData.length > 0;
    };

    $scope.reloadFaultCountGraph = function() {
      var btxnFaultData = [];
      _.each($scope.businessTransactions, (btxn: any) => {
        if (btxn.faultcount) {
          btxnFaultData.push([btxn.name, btxn.faultcount]);
        }
      });

      $scope.btxnfaultcountpiechart.load({
        columns: btxnFaultData
      });

      $scope.faultChartHasData = btxnFaultData.length > 0;
    };

  }]);

}
