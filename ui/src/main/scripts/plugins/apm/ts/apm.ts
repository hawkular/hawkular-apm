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

/// <reference path="apmPlugin.ts"/>
module APM {

  declare let c3: any;

  export let APMController = _module.controller('APM.APMController', ['$scope', '$rootScope', '$routeParams', '$http',
    '$interval', '$timeout', ($scope, $rootScope, $routeParams, $http, $interval, $timeout) => {

    $scope.config = {
      interval: '60000000',
      maxRows: 10
    };

    $scope.nodeComponents = [];

    $scope.components = [];

    $scope.timestamps = ['timestamp'];

    $scope.lists = {};

    $scope.apmChartConfig = {
      data: {
        columns: $scope.nodeComponents || [],
        type: 'area',
        x: 'timestamp',
        keys: {
          value: $scope.components || [],
          x: 'timestamp'
        },
        groups: [$scope.componentsGroups || []]
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
          default: [0, 10000000000],
          padding: {bottom: 0},
          tick: {
            format: function (y) { return y / 1000000; }
          }
        }
      }
    };

    $scope.chartStacked = true;

    $scope.reloadData = function() {
      $rootScope.updateCriteriaTimeSpan();

      $http.get('/hawkular/apm/analytics/node/statistics?interval=' +
        $scope.config.interval + '&criteria=' + encodeURI(angular.toJson($rootScope.sbFilter.criteria)))
          .then(function(resp) {

        // get all component keys
        let components = {};
        let timestamps = ['timestamp'];
        let nodeComponents = [];
        let componentTypes = [];

        _.forEach(resp.data, (datapoint: any) => {
            datapoint.timestamp = datapoint.timestamp / 1000; // Convert from micro to milliseconds
            timestamps.push(datapoint.timestamp);

            // Iterate and gather the available component types in this datapoint
            _.forEach(Object.keys(datapoint.componentTypes), (compType) => {
              if (!components[compType]) {
                componentTypes.push(compType);
                // in case there's datapoints processed already, we fill the new with so many 0's
                components[compType] = _.fill(Array(timestamps.length - 1), 0);
                // and the first entry is always the component type name
                components[compType][0] = compType;
                nodeComponents.push(components[compType]);
              }
            });

            // Add 0 value for all types, in case they are not present
            _.forEach(nodeComponents, (nodeComponent: any) => {
              nodeComponent.push(0);
            });

            // in case they are present, will now be replaced by correct value
            _.forEach(datapoint.componentTypes, (value: any, component: string) => {
                components[component][components[component].length - 1] = value.duration;
            });
        });

        nodeComponents.unshift(timestamps);

        $scope.nodeComponents = nodeComponents;
        $scope.components = componentTypes;
        $scope.componentsGroups = !$scope.chartStacked ? [] :
          $scope.components.length > 0 ? $scope.components : [];

        $scope.redrawAreaChart();

      },function(resp) {
        console.log('Failed to get node timeseries statistics: ' + angular.toJson(resp));
      });

      $http.get('/hawkular/apm/analytics/node/summary?criteria=' +
          encodeURI(angular.toJson($rootScope.sbFilter.criteria))).then(function(resp) {
        $scope.summaries = resp.data;

        $scope.max = 0;

        for (let i = 0; i < $scope.summaries.length; i++) {
          if ($scope.summaries[i].elapsed > $scope.max) {
            $scope.max = $scope.summaries[i].elapsed;
          }
        }
      },function(resp) {
        console.log('Failed to get node summary statistics: ' + angular.toJson(resp));
      });

      // this informs the sidebar directive, so it'll update it's data as well
      $scope.$broadcast('dataUpdated');
    };

    $rootScope.$watch('sbFilter.customStartTime', $scope.reloadData);
    $rootScope.$watch('sbFilter.customEndTime', $scope.reloadData);

    $scope.redrawAreaChart = function() {
      $scope.apmChartConfig.data.columns = $scope.nodeComponents;
      $scope.apmChartConfig.data.groups = [$scope.componentsGroups || []];
      $scope.apmChartConfig.data.keys.value = $scope.components || [];
    };

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
        if ($scope.areaChart) {
          $scope.areaChart.flush();
        }
      });
    });

    $scope.getAreaChart = function(theChart) {
      $scope.areaChart = theChart;
    };

    $scope.toggleStacked = function() {
      $scope.chartStacked = !$scope.chartStacked;
      $scope.componentsGroups = $scope.chartStacked ? $scope.components : [];
      $scope.apmChartConfig.data.groups = [$scope.componentsGroups];
    };

    $scope.pauseLiveData = function() {
      $rootScope.sbFilter.criteria.endTime = $rootScope.sbFilter.criteria.endTime === '0' ? ('' + +new Date()) : '0';
      $scope.reloadData();
    };

    $scope.currentDateTime = function() {
      return new Date();
    };

    $scope.getActualPercentage = function(summary) {
      return Math.round((summary.actual / $scope.max) * 100);
    };

    $scope.getElapsedMinusActualPercentage = function(summary) {
      return Math.round(((summary.elapsed - summary.actual) / $scope.max) * 100);
    };

    $scope.getElapsedPercentage = function(summary) {
      return Math.round((summary.elapsed / $scope.max) * 100);
    };

    $scope.sort = function(keyname){
      $scope.sortKey = keyname;   //set the sortKey to the param passed
      $scope.reverse = !$scope.reverse; //if true make it false and vice versa
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
