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

/// <reference path="servicesPlugin.ts"/>
module Services {

  declare let c3: any;

  export let ServicesController = _module.controller('Services.ServicesController', ['$scope', '$rootScope', '$http',
    '$q', '$interval', '$timeout', ($scope, $rootScope, $http, $q, $interval, $timeout) => {

    $scope.services = [];
    $scope.buildStamps = [];

    $scope.getBuildStamps = function() {
      $scope.buildStamp = undefined;
      $scope.buildStamps = _.find($scope.services, 'name', $scope.service.name)['buildStamps'];
    };

    $scope.selectedServices = [];

    $scope.addService = function() {
      $scope.selectedServices.push({'service': $scope.service, 'buildStamp': $scope.buildStamp});
      delete $scope.service;
      delete $scope.buildStamp;
    };

    $scope.remService = function(index) {
      $scope.selectedServices.splice(index, 1);
    };

    let config = {
      headers: { 'Hawkular-Tenant': 'hawkular' }
    };

    $scope.getServices = function() {
      $http.get('/hawkular/apm/services?interval=' + $scope.config.interval +
        '&criteria=' + encodeURI(angular.toJson($rootScope.sbFilter.criteria)), config).then((resp) => {
          $scope.services = resp.data;
        }, (resp) => {
          console.log('Failed to get services list: ' + angular.toJson(resp));
        });
    };

    $scope.getServices();

    $scope.getServiceData = function(service) {
      let txnCriteria = angular.copy($rootScope.sbFilter.criteria);
      // txnCriteria.transaction = $scope.transactionName; // adjust sidebar criteria with txn name

      let tmpStatistics = [];
      let tmpTxFaultData = [];

      let txList = ['List My Orders', 'Place Order'];

      let successFn = function(txn, resp) {
        _.forEach(resp.data, (datapoint: any) => {
          datapoint.timestamp = datapoint.timestamp / 1000; // Convert from micro to milliseconds
        });

        let chtDT = _.map(resp.data, 'average');
        chtDT.splice(0, 0, txn);
        if (tmpStatistics.length === 0) {
          let chtTS = _.map(resp.data, 'timestamp');
          chtTS.splice(0, 0, 'timestamp');
          tmpStatistics.push(chtTS, chtDT);
        } else {
          tmpStatistics.push(chtDT);
        }

        let chtTC = _.map(resp.data, 'count');
        chtTC.splice(0, 0, txn + ' Count');
        let chtFC = _.map(resp.data, 'faultCount');
        chtFC.splice(0, 0, txn + ' Faults');
        if (tmpTxFaultData.length === 0) {
          let chtTS = _.map(resp.data, 'timestamp');
          chtTS.splice(0, 0, 'timestamp');
          tmpTxFaultData.push(chtTS, chtTC, chtFC);
        } else {
          tmpTxFaultData.push(chtTC, chtFC);
        }
      };

      let errorFn = function(resp) {
        console.log('Failed to get statistics: ' + angular.toJson(resp));
      };

      let promises = [];
      _.forEach(txList, (txn) => {
        txnCriteria.transaction = txn;
        promises.push(
          $http.get('/hawkular/apm/analytics/trace/completion/statistics?interval=' + $scope.config.interval +
          '&criteria=' + encodeURI(angular.toJson(txnCriteria))).then(successFn.bind(null, txn),
          errorFn));
      });

      $q.all(promises).then(() => {
        $scope.statistics = tmpStatistics;
        $scope.tfData = tmpTxFaultData;
        $scope.redrawCompRTChart();
        $scope.redrawCompTFChart();
      });
    };

    $scope.statistics = [];
    $scope.rtChartConfig = {
      data: {
        x: 'timestamp',
        columns: [],
        type: 'area'
      },
      // color: {
      //   pattern: ['#ff0000', '#33cc33', '#e5e600', '#99ccff', '#ffb3b3']
      // },
      axis: {
        x: {
          type: 'timeseries',
          tick: {
            culling: {
              max: 6 // the number of tick texts will be adjusted to less than this value
            },
            format: '%Y-%m-%d %H:%M:%S'
          }
        },
        y: {
          label: 'Seconds',
          padding: { bottom: 0 },
          tick: {
            format: function(y) { return y / 1000000; }
          },
        },
      }
    };

    $scope.tfChartConfig = {
      data: {
        x: 'timestamp',
        columns: [],
        type: 'bar',
        groups: []
      },
      color: {
        pattern: [
          '#1f77b4', '#aec7e8',
          '#ff7f0e', '#ffbb78',
          '#2ca02c', '#98df8a',
          '#d62728', '#ff9896',
          '#9467bd', '#c5b0d5',
          '#8c564b', '#c49c94',
          '#e377c2', '#f7b6d2',
          '#7f7f7f', '#c7c7c7',
          '#bcbd22', '#dbdb8d',
          '#17becf', '#9edae5'
        ]
      },
      axis: {
        x: {
          type: 'timeseries',
          tick: {
            culling: {
              max: 6 // the number of tick texts will be adjusted to less than this value
            },
            format: '%Y-%m-%d %H:%M:%S'
          }
        },
        y: {
          label: 'Count',
          padding: { bottom: 0 },
        },
      }
    };

    $scope.redrawCompRTChart = function() {
      $scope.rtChartConfig.data.columns = $scope.statistics;
    };

    $scope.redrawCompTFChart = function() {
      let txGroups = _.values(_.groupBy(_.map($scope.tfData.slice(1), (arr) => { return arr[0]; }), (item, i) => {
        return Math.floor(i / 2);
      }));

      $scope.tfChartConfig.data.columns = $scope.tfData;
      $scope.tfChartConfig.data.groups = txGroups;
    };

    $scope.config = {
      interval: '60000000',
      maxRows: 10
    };

    $scope.reloadData = function() {
      $rootScope.updateCriteriaTimeSpan();

      $scope.getServiceData();

      // this informs the sidebar directive, so it'll update it's data as well
      $scope.$broadcast('dataUpdated');
    };

    $rootScope.$watch('sbFilter.customStartTime', $scope.reloadData);
    $rootScope.$watch('sbFilter.customEndTime', $scope.reloadData);

    let refreshPromise = $interval(() => {
      if ($rootScope.sbFilter.criteria.endTime === '0') {
        $scope.reloadData();
      }
    }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $rootScope.$watch('sbFilter.criteria', $scope.reloadData, true);
    $scope.$watch('config', $scope.reloadData, true);

    // watch for sidebar changes, to redraw the area chart
    $scope.hideSidebar = false;
    $scope.$watch('hideSidebar', function() {
      $timeout(function() {
        if ($scope.rtChart) {
          $scope.rtChart.flush();
        }
        if ($scope.tfChart) {
          $scope.tfChart.flush();
        }
      });
    });

    $scope.getCompRTChart = function(theChart) {
      $scope.rtChart = theChart;
    };

    $scope.getCompTFChart = function(theChart) {
      $scope.tfChart = theChart;
    };

    $scope.pauseLiveData = function() {
      $rootScope.sbFilter.criteria.endTime = $rootScope.sbFilter.criteria.endTime === '0' ? ('' + +new Date()) : '0';
      $scope.reloadData();
    };

    // $scope.currentDateTime = function() {
    //   return new Date();
    // };

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

    $scope.getServiceData();

  }]);
}
