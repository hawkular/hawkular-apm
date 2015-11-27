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

  declare var c3: any;

  export var BTMController = _module.controller("BTM.BTMController", ["$scope", "$http", '$location', '$interval', ($scope, $http, $location, $interval) => {

    $scope.newBTxnName = '';
    $scope.candidateCount = 0;
    
    $scope.chart = "None";
    
    $scope.txnCountValues = [];
    $scope.faultCountValues = [];

    $scope.reload = function() {
      $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
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
        console.log("Failed to get business txn summaries: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/unbounduris').then(function(resp) {
        $scope.candidateCount = Object.keys(resp.data).length;
      },function(resp) {
        console.log("Failed to get candidate count: "+JSON.stringify(resp));
      });
    };

    $scope.reload();

    $interval(function() {
      $scope.reload();
    },10000);

    $scope.getBusinessTxnDetails = function(btxn) {
      $http.get('/hawkular/btm/analytics/completion/count?businessTransaction='+btxn.summary.name).then(function(resp) {
        btxn.count = resp.data;
        
        $scope.reloadTxnCountGraph();

      },function(resp) {
        console.log("Failed to get count: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/completion/percentiles?businessTransaction='+btxn.summary.name).then(function(resp) {
        if (resp.data.percentiles[95] > 0) {
          btxn.percentile95 = Math.round( resp.data.percentiles[95] / 1000000 ) / 1000;
        } else {
          btxn.percentile95 = 0;
        }
      },function(resp) {
        console.log("Failed to get completion percentiles: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/completion/faultcount?businessTransaction='+btxn.summary.name).then(function(resp) {
        btxn.faultcount = resp.data;
        
        $scope.reloadFaultCountGraph();

      },function(resp) {
        console.log("Failed to get fault count: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/alerts/count/'+btxn.summary.name).then(function(resp) {
        btxn.alerts = resp.data;
      },function(resp) {
        console.log("Failed to get alerts count: "+JSON.stringify(resp));
      });
    };

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

    $scope.initGraph = function() {
      $scope.btxncountpiechart = c3.generate({
        bindto: '#btxntxncountpiechart',
        data: {
          json: [
          ],
          type: 'pie',
          onclick: function (d, i) {
            $location.path('info/'+d.id);
          }
        }
      });

      $scope.btxnfaultcountpiechart = c3.generate({
        bindto: '#btxnfaultcountpiechart',
        data: {
          json: [
          ],
          type: 'pie',
          onclick: function (d, i) {
            $location.path('info/'+d.id);
          }
        }
      });
    };
    
    $scope.reloadTxnCountGraph = function() {
      var removeTxnCountValues = angular.copy($scope.txnCountValues);

      var btxndata = [];

      for (var i = 0; i < $scope.businessTransactions.length; i++) {
        var btxn = $scope.businessTransactions[i];
        if (btxn.count !== undefined && btxn.count > 0) {
          var record=[ ];
          record.push(btxn.summary.name);
          record.push(btxn.count);
          btxndata.push(record);

          if ($scope.txnCountValues.contains(btxn.summary.name)) {
            removeTxnCountValues.remove(btxn.summary.name);
          } else {
            $scope.txnCountValues.add(btxn.summary.name);
          }
        }
      }

      $scope.btxncountpiechart.load({
        columns: btxndata
      });

      for (var j=0; j < removeTxnCountValues.length; j++) {
        $scope.btxncountpiechart.unload(removeTxnCountValues[j]);
        $scope.txnCountValues.remove(removeTxnCountValues[j]);
      }
    };

    $scope.reloadFaultCountGraph = function() {
      var removeFaultCountValues = angular.copy($scope.faultCountValues);

      var btxnfaultdata = [];

      for (var i = 0; i < $scope.businessTransactions.length; i++) {
        var btxn = $scope.businessTransactions[i];
        if (btxn.faultcount !== undefined && btxn.faultcount > 0) {
          var record=[ ];
          record.push(btxn.summary.name);
          record.push(btxn.faultcount);
          btxnfaultdata.push(record);

          if ($scope.faultCountValues.contains(btxn.summary.name)) {
            removeFaultCountValues.remove(btxn.summary.name);
          } else {
            $scope.faultCountValues.add(btxn.summary.name);
          }
        }
      }

      $scope.btxnfaultcountpiechart.load({
        columns: btxnfaultdata
      });

      for (var j=0; j < removeFaultCountValues.length; j++) {
        $scope.btxnfaultcountpiechart.unload(removeFaultCountValues[j]);
        $scope.faultCountValues.remove(removeFaultCountValues[j]);
      }
    };
    
    $scope.initGraph();

  }]);

}
