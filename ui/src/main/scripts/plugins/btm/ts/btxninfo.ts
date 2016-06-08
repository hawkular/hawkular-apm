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

  declare let c3: any;

  export let BTxnInfoController = _module.controller('BTM.BTxnInfoController', ['$scope', '$routeParams', '$http',
    '$interval', '$timeout', 'btxn', ($scope, $routeParams, $http, $interval, $timeout, btxn) => {

    $scope.btxn = btxn;
    $scope.businessTransactionName = $routeParams.businesstransaction;

    $scope.properties = [];

    $scope.propertyValues = [];
    $scope.faultValues = [];

    $scope.criteria = {
      businessTransaction: $scope.businessTransactionName,
      properties: [],
      faults: [],
      startTime: '-3600000',
      endTime: '0',
      lowerBound: 0
    };

    $scope.config = {
      interval: '60000',
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
            format: function(y) { return y / 1000; }
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

    let addIfAbsent = function(filterList, newPropName, newPropValue) {
      let found = false;
      _.each(filterList, (prop: any) => {
        if (prop.name === newPropName && prop.value === newPropValue) {
          found = true;
        }
      });
      if (!found) {
        filterList.push({name: newPropName, value: newPropValue});
      }
      return !found;
    };

    $scope.faultData = [];
    $scope.ctFaultChartConfig = {
      data: {
        columns: $scope.faultData,
        type: 'pie',
        onclick: function(d, i) {
          let added = addIfAbsent($scope.criteria.faults, undefined, d.id);
          if (added) {
            $scope.reload();
          }
        }
      }
    };

    $scope.propertyData = [];
    $scope.ctPropChartConfig = {
      data: {
        columns: $scope.propertyData,
        type: 'pie',
        onclick: function(d, i) {
          let added = addIfAbsent($scope.criteria.properties, $scope.config.selectedProperty, d.id);
          if (added) {
            $scope.reload();
          }
        }
      }
    };

    $scope.reload = function() {
      $http.post('/hawkular/apm/analytics/trace/completion/statistics?interval=' + $scope.config.interval,
          $scope.criteria).then(function(resp) {
        $scope.statistics = resp.data;
        $scope.updatedBounds();
        $scope.redrawLineChart();
      },function(resp) {
        console.log('Failed to get statistics: ' + JSON.stringify(resp));
      });

      let faultCriteria = angular.copy($scope.criteria);
      faultCriteria.maxResponseSize = $scope.config.maxFaultValues;

      $http.post('/hawkular/apm/analytics/trace/completion/faults', faultCriteria).then(function(resp) {
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
        console.log('Failed to get statistics: ' + JSON.stringify(resp));
      });

      $http.post('/hawkular/apm/analytics/properties', $scope.criteria).then(
      function(resp) {
        $scope.properties = resp.data;
      },
      function(resp) {
        console.log('Failed to get property info: ' + JSON.stringify(resp));
      });

      if ($scope.config.selectedProperty !== undefined) {
        $scope.reloadProperty();
      }
    };

    $scope.redrawLineChart = function() {
      $scope.compTimeChartConfig.data.json = $scope.statistics;
    };

    $scope.reloadProperty = function() {
      let propertyCriteria = angular.copy($scope.criteria);
      propertyCriteria.maxResponseSize = $scope.config.maxPropertyValues;

      $http.post('/hawkular/apm/analytics/trace/completion/property/' + $scope.config.selectedProperty,
          propertyCriteria).then(function(resp) {
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
          JSON.stringify(resp));
      });
    };

    $scope.reload();

    let refreshPromise = $interval(() => {
      if ($scope.criteria.endTime === '0' || $scope.config.prevLowerBoundDisplay !== $scope.config.lowerBoundDisplay) {
        $scope.reload();

        $scope.config.prevLowerBoundDisplay = $scope.config.lowerBoundDisplay;
      }
    }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.removeProperty = function(property) {
      $scope.criteria.properties.remove(property);
      $scope.reload();
    };

    $scope.removeFault = function(fault) {
      $scope.criteria.faults.remove(fault);
      $scope.reload();
    };

    $scope.toggleExclusion = function(element) {
      element.excluded = !element.excluded;
      $scope.reload();
    };

    $scope.updatedBounds = function() {
      if ($scope.config.lowerBoundDisplay === 0) {
        $scope.criteria.lowerBound = 0;
      } else {
        let maxDuration: any = (_.max($scope.statistics, 'max') as any).max;
        if (maxDuration > 0) {
          $scope.criteria.lowerBound = ( $scope.config.lowerBoundDisplay * maxDuration ) / 100;
        }
      }
    };

    $scope.selectAction = function() {
      $scope.reload();
    };

    $scope.pauseLiveData = function() {
      $scope.criteria.endTime = $scope.criteria.endTime === '0' ? ('' + +new Date()) : '0';
      $scope.reload();
    };

    $scope.currentDateTime = function() {
      return new Date();
    };

  }]);
}
