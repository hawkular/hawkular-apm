///
/// Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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

  declare let c3: any;

  export let BTMController = _module.controller('BTM.TxnController',['$scope', '$rootScope', '$http', '$location',
     '$interval', '$q', '$timeout', ($scope, $rootScope, $http, $location, $interval, $q, $timeout) => {

    $scope.candidateCount = 0;

    $scope.transactions = [];

    let redirectToInfo = function(txn) {
      $timeout(() => {
        $location.path('/hawkular-ui/apm/btm/info/' + txn.id);
      });
    };

    $scope.countChartConfig = {
      data: {
        type: 'pie',
        columns: $scope.txnCountData || [],
        onclick: redirectToInfo
      }
    };

    $scope.faultChartConfig = {
      data: {
        type: 'pie',
        columns: $scope.txnFaultData || [],
        onclick: redirectToInfo
      }
    };

    $scope.criteria = {
      startTime: -3600000
    };

    $scope.reloadData = function() {

      // adjust criteria to remove "hostname" and "transaction" as they are hidden from sidebar
      let adjCriteria = angular.copy($rootScope.sbFilter.criteria);
      adjCriteria.transaction = '';
      adjCriteria.hostName = '';

      $http.get('/hawkular/apm/analytics/transactions?criteria='
          + encodeURI(angular.toJson(adjCriteria))).then(function(resp) {

        let allPromises = [];
        _.each(resp.data, (txn: any) => {
          allPromises = allPromises.concat($scope.getTxnDetails(txn));
        });

        $q.all(allPromises).then(() => {
          $scope.transactions = resp.data;
          $scope.transactions.$resolved = true;

          $scope.reloadTxnCountGraph();
          $scope.reloadFaultCountGraph();
        });
      },function(resp) {
        console.log('Failed to get transaction summaries: ' + angular.toJson(resp));
      });

      $http.get('/hawkular/apm/analytics/unboundendpoints').then(function(resp) {
        $scope.candidateCount = Object.keys(resp.data).length;
      },function(resp) {
        console.log('Failed to get candidate count: ' + angular.toJson(resp));
      });

      // this informs the sidebar directive, so it'll update it's data as well
      $scope.$broadcast('dataUpdated');
    };

    $rootScope.$watch('sbFilter.criteria', $scope.reloadData, true);
    $scope.$watch('config', $scope.reloadData, true);

    $scope.reloadData();

    let refreshPromise = $interval(() => { $scope.reloadData(); }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.getTxnDetails = function(txn) {
      let promises = [];

      let txncriteria = angular.copy($rootScope.sbFilter.criteria);
      txncriteria.transaction = txn.name;
      txncriteria.hostName = ''; // as we don't show it on sidebar

      let countPromise = $http.get('/hawkular/apm/analytics/trace/completion/count?criteria='
          + encodeURI(angular.toJson(txncriteria)));
      promises.push(countPromise);
      countPromise.then(function(resp) {
        txn.count = resp.data;
      }, function(resp) {
        console.log('Failed to get count: ' + angular.toJson(resp));
      });

      let pct95Promise =
        $http.get('/hawkular/apm/analytics/trace/completion/percentiles?criteria='
          + encodeURI(angular.toJson(txncriteria)));
      promises.push(pct95Promise);
      pct95Promise.then(function(resp) {
        if (resp.data.percentiles[95] > 0) {
          txn.percentile95 = (resp.data.percentiles[95] / 1000000).toFixed(3);
        } else {
          txn.percentile95 = 0;
        }
      },function(resp) {
        console.log('Failed to get completion percentiles: ' + angular.toJson(resp));
      });

      let faultsPromise =
        $http.get('/hawkular/apm/analytics/trace/completion/faultcount?criteria='
          + encodeURI(angular.toJson(txncriteria)));
      promises.push(faultsPromise);
      faultsPromise.then(function(resp) {
        txn.faultcount = resp.data;
      },function(resp) {
        console.log('Failed to get fault count: ' + angular.toJson(resp));
      });

      return promises;
    };

    $scope.deleteTxn = function(txn) {
      if (confirm('Are you sure you want to delete transaction \'' + txn.name + '\'?')) {
        $http.delete('/hawkular/apm/config/transaction/full/' + txn.name).then(function(resp) {
          console.log('Deleted: ' + txn.name);
          $scope.transactions.remove(txn);
        },function(resp) {
          console.log('Failed to delete transaction \'' + txn.name + '\': ' + angular.toJson(resp));
        });
      }
    };

    $scope.getTxnsChart = function(theChart) {
      $scope.txnsChart = theChart;
    };

    $scope.getFaultsChart = function(theChart) {
      $scope.faultsChart = theChart;
    };

    // watch for sidebar changes, to redraw the charts
    $scope.$watch('hideSidebar', function() {
      $timeout(function() {
        if ($scope.txnsChart) {
          $scope.txnsChart.flush();
        }
        if ($scope.faultsChart) {
          $scope.faultsChart.flush();
        }
      });
    });

    $scope.reloadTxnCountGraph = function() {
      let txnCountData = [];
      _.each($scope.transactions, (txn: any) => {
        if (txn.count) {
          txnCountData.push([txn.name, txn.count]);
        }
      });

      $scope.countChartConfig.data.columns = txnCountData;
      $scope.countChartHasData = txnCountData.length > 0;
    };

    $scope.reloadFaultCountGraph = function() {
      let txnFaultData = [];
      _.each($scope.transactions, (txn: any) => {
        if (txn.faultcount) {
          txnFaultData.push([txn.name, txn.faultcount]);
        }
      });

      $scope.faultChartConfig.data.columns = txnFaultData;
      $scope.faultChartHasData = txnFaultData.length > 0;
    };

    $scope.addPropertyToFilter = function(pName, pValue, operator) {
      let newProp = {name: pName, value: pValue, operator: operator};
      $rootScope.sbFilter.criteria.properties.push(newProp);
      delete $scope.selPropValue;
    };

    $scope.remPropertyFromFilter = function(property) {
      $rootScope.sbFilter.criteria.properties.splice($rootScope.sbFilter.criteria.properties.indexOf(property), 1);
    };

    $scope.toggleExcludeInclude = function(propOrFault) {
      if (propOrFault.operator === undefined || propOrFault.operator === 'HAS') {
        propOrFault.operator = 'HASNOT';
      } else if (propOrFault.operator === 'HASNOT') {
        propOrFault.operator = 'HAS';
      }
    };

  }]);

}
