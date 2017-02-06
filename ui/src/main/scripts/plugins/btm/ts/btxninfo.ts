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

  export let TxnInfoController = _module.controller('BTM.TxnInfoController', ['$scope', '$rootScope', '$routeParams',
    '$http', '$interval', '$timeout', 'txn', ($scope, $rootScope, $routeParams, $http, $interval, $timeout, txn) => {

    let oldSidebarTX = $rootScope.sbFilter.criteria.transaction;
    $rootScope.sbFilter.criteria.transaction = $routeParams.transaction;

    $scope.txn = txn;
    $scope.transactionName = $routeParams.transaction;

    $scope.properties = [];

    $scope.propertyValues = [];
    $scope.faultValues = [];

    $scope.config = {
      interval: '60000000',
      selectedProperty: undefined,
      lowerBoundDisplay: 0,
      prevLowerBoundDisplay: 0,
      maxPropertyValues: 20,
      maxFaultValues: 20
    };

    $scope.statistics = [];
    $scope.compTimeChartConfig = {
      data: {
        json: $scope.statistics,
        axes: {
          max: 'y',
          average: 'y',
          min: 'y',
          count: 'y2',
          faultCount: 'y2'
        },
        type: 'line',
        types: {
          count: 'bar',
          faultCount: 'bar'
        },
        keys: {
          value: ['max', 'average', 'min', 'count', 'faultCount'],
          x: 'timestamp'
        }
      },
      color: {
        pattern: ['#ff0000', '#33cc33', '#e5e600', '#99ccff', '#ffb3b3']
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
          label: 'Seconds',
          default: [0, 10000],
          padding: { bottom: 0 },
          tick: {
            format: function(y) { return y / 1000000; }
          }
        },
        y2: {
          show: true
        }
      }
    };

    $scope.getPropsChart = function(theChart) {
      $scope.propsChart = theChart;
    };

    $scope.getFaultsChart = function(theChart) {
      $scope.faultsChart = theChart;
    };

    $scope.getCompTimeChart = function(theChart) {
      $scope.compTimeChart = theChart;
    };

    let chartOnClick = function(name, value) {
      $timeout(() => {
        let wasToggled = false;
        _.forEach($rootScope.sbFilter.criteria.properties, function(prop) {
          if (prop.name === name && prop.value === value) {
            $scope.toggleExcludeInclude(prop);
            wasToggled = true;
          }
        });
        if (!wasToggled) {
          $scope.addPropertyToFilter(name, value);
        }
      }, 0);
    };

    $scope.faultData = [];
    $scope.ctFaultChartConfig = {
      data: {
        columns: $scope.faultData,
        type: 'pie',
        onclick: function(d, i) {
          chartOnClick('fault', d.id);
        }
      }
    };

    $scope.propertyData = [];
    $scope.ctPropChartConfig = {
      data: {
        columns: $scope.propertyData,
        type: 'pie',
        onclick: function(d, i) {
          chartOnClick($scope.config.selectedProperty, d.id);
        }
      }
    };

    $scope.reloadData = function() {
      let txnCriteria = angular.copy($rootScope.sbFilter.criteria);
      txnCriteria.transaction = $scope.transactionName; // adjust sidebar criteria with txn name
      txnCriteria.hostName = ''; // as we don't show it on sidebar

      $http.get('/hawkular/apm/analytics/trace/completion/statistics?interval=' + $scope.config.interval +
          '&criteria=' + encodeURI(angular.toJson(txnCriteria))).then(function(resp) {
        _.forEach(resp.data, (datapoint: any) => {
            datapoint.timestamp = datapoint.timestamp / 1000; // Convert from micro to milliseconds
        });
        $scope.statistics = resp.data;
        $scope.updatedBounds();
        $scope.redrawLineChart();
      },function(resp) {
        console.log('Failed to get statistics: ' + angular.toJson(resp));
      });

      let faultCriteria = angular.copy(txnCriteria);
      faultCriteria.maxResponseSize = $scope.config.maxFaultValues;

      $http.get('/hawkular/apm/analytics/trace/completion/faults?criteria=' +
          encodeURI(angular.toJson(faultCriteria))).then(function(resp) {
        let removeFaultValues = angular.copy($scope.faultValues || []);
        $scope.faults = resp.data;

        let faultdata = [];

        _.each($scope.faults, (fault: any) => {
          faultdata.push([fault.value, fault.count]);

          if ($scope.faultValues.indexOf(fault.value) === -1) {
            $scope.faultValues.add(fault.value);
          } else {
            removeFaultValues.remove(fault.value);
          }
        });

        if($scope.faultsChart) {
          $timeout(() => {
            $scope.faultsChart.load({ columns: faultdata });

            _.each(removeFaultValues, (remValue) => {
              $scope.faultsChart.unload(remValue);
              $scope.faultValues.remove(remValue);
            });
          });
        } else {
          $scope.ctFaultChartConfig.data.columns = faultdata;
        }
      },function(resp) {
        console.log('Failed to get statistics: ' + angular.toJson(resp));
      });

      $http.get('/hawkular/apm/analytics/properties?from=ch&criteria=' +
          encodeURI(angular.toJson(faultCriteria))).then(
      function(resp) {
        $scope.properties = resp.data;
      },
      function(resp) {
        console.log('Failed to get property info: ' + angular.toJson(resp));
      });

      if ($scope.config.selectedProperty !== undefined) {
        $scope.reloadProperty();
      }
      // this informs the sidebar directive, so it'll update it's data as well
      $scope.$broadcast('dataUpdated');
    };

    $scope.redrawLineChart = function() {
      $scope.compTimeChartConfig.data.json = $scope.statistics;
    };

    $scope.reloadProperty = function() {
      let propertyCriteria = angular.copy($rootScope.sbFilter.criteria);
      propertyCriteria.maxResponseSize = $scope.config.maxPropertyValues;

      $http.get('/hawkular/apm/analytics/trace/completion/property/' + $scope.config.selectedProperty +
          '?criteria=' + encodeURI(angular.toJson(propertyCriteria))).then(function(resp) {
        let removePropValues = angular.copy($scope.propertyValues || []);
        $scope.propertyDetails = resp.data;

        let propertydata = [];

        _.each($scope.propertyDetails, (prop: any) => {
          propertydata.push([prop.value, prop.count]);

          if ($scope.propertyValues.indexOf(prop.value) === -1) {
            $scope.propertyValues.add(prop.value);
          } else {
            removePropValues.remove(prop.value);
          }
        });

        if ($scope.propsChart) {
          $scope.propsChart.load({ columns: propertydata });

          _.each(removePropValues, (remValue) => {
            $scope.propsChart.unload(remValue);
            $scope.propertyValues.remove(remValue);
          });
        } else {
          $scope.ctPropChartConfig.data.columns = propertydata;
        }

      },function(resp) {
        console.log('Failed to get property details for \'' + $scope.config.selectedProperty + '\': ' +
          angular.toJson(resp));
      });
    };

    let refreshPromise = $interval(() => {
      if ($rootScope.sbFilter.criteria.endTime === '0' ||
        $scope.config.prevLowerBoundDisplay !== $scope.config.lowerBoundDisplay) {

        $scope.reloadData();

        $scope.config.prevLowerBoundDisplay = $scope.config.lowerBoundDisplay;
      }
    }, 10000);
    $scope.$on('$destroy', () => {
      $rootScope.sbFilter.criteria.transaction = oldSidebarTX;
      delete $rootScope.sbFilter.criteria.lowerBound;
      $interval.cancel(refreshPromise);
    });

    $scope.updatedBounds = function() {
      if ($scope.config.lowerBoundDisplay === 0) {
        $rootScope.sbFilter.criteria.lowerBound = 0;
      } else {
        let maxDuration: any = (_.max($scope.statistics, 'max') as any).max;
        if (maxDuration > 0) {
          $rootScope.sbFilter.criteria.lowerBound = ( $scope.config.lowerBoundDisplay * maxDuration ) / 100;
        }
      }
    };

    $scope.pauseLiveData = function() {
      $rootScope.sbFilter.criteria.endTime = $rootScope.sbFilter.criteria.endTime === '0' ? ('' + +new Date()) : '0';
      $scope.reloadData();
    };

    $scope.currentDateTime = function() {
      return new Date();
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

    $rootScope.$watch('sbFilter.criteria', $scope.reloadData, true);

    // watch for sidebar changes, to redraw the charts
    $scope.$watch('hideSidebar', function() {
      $timeout(function() {
        if ($scope.compTimeChart) {
          $scope.compTimeChart.flush();
        }
        if ($scope.propsChart) {
          $scope.propsChart.flush();
        }
        if ($scope.faultsChart) {
          $scope.faultsChart.flush();
        }
      });
    });

  }]);
}
