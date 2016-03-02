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

/// <reference path="apmPlugin.ts"/>
module APM {

  declare let c3: any;

  export let APMController = _module.controller('APM.APMController', ['$scope', '$routeParams', '$http', '$location',
    '$interval', ($scope, $routeParams, $http, $location, $interval) => {

    $scope.criteria = {
      businessTransaction: undefined,
      hostName: $routeParams.hostName,
      properties: [],
      startTime: '-3600000',
      endTime: '0'
    };

    $scope.config = {
      interval: '60000',
      maxRows: 10
    };

    $scope.nodeComponents = [];

    $scope.businessTransactions = [];

    $scope.components = [];

    $scope.timestamps = ['timestamp'];

    $scope.lists = {};

    $scope.businessTransactions = [];

    $scope.chartStacked = true;

    $scope.reloadData = function() {

      $http.post('/hawkular/btm/analytics/node/statistics?interval=' +
        $scope.config.interval, $scope.criteria).then(function(resp) {

        // get all component keys
        let components = {};
        let timestamps = ['timestamp'];
        let nodeComponents = [];
        let componentTypes = [];

        _.forEach(resp.data, (datapoint: any) => {
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

        let firstData = $scope.components.length === 0 && componentTypes.length > 0;
        let lastData = $scope.components.length > 0 && componentTypes.length === 0;

        $scope.nodeComponents = nodeComponents;
        $scope.components = componentTypes;
        $scope.componentsGroups = !$scope.chartStacked ? [] :
          $scope.components.length > 0 ? $scope.components : [];

        // have to initialize to pick stacked/layered option and also to empty the chart
        // when there's no more data to show, otherwise it stalls at last data
        if (!$scope.nodesareachart || lastData || (firstData && $scope.chartStacked)) {
          $scope.initGraph();
        }

        $scope.redrawAreaChart();

      },function(resp) {
        console.log('Failed to get node timeseries statistics: ' + JSON.stringify(resp));
      });

      $http.post('/hawkular/btm/analytics/node/summary', $scope.criteria).then(function(resp) {
        $scope.summaries = resp.data;

        $scope.max = 0;

        for (let i = 0; i < $scope.summaries.length; i++) {
          if ($scope.summaries[i].elapsed > $scope.max) {
            $scope.max = $scope.summaries[i].elapsed;
          }
        }
      },function(resp) {
        console.log('Failed to get node summary statistics: ' + JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
        $scope.businessTransactions.length = 0;

        for (let i = 0; i < resp.data.length; i++) {
          $scope.businessTransactions.add(resp.data[i].name);
        }
      },function(resp) {
        console.log('Failed to get business txn summaries: ' + JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/hostnames').then(function(resp) {
        $scope.hostNames = [ ];
        for (let i = 0; i < resp.data.length; i++) {
          $scope.hostNames.add(resp.data[i]);
        }
      },function(resp) {
        console.log('Failed to get host names: ' + JSON.stringify(resp));
      });
    };

    $scope.redrawAreaChart = function() {
      $scope.nodesareachart.load({
        columns: $scope.nodeComponents,
        keys: {
          value: $scope.components,
          x: 'timestamp'
        },
        groups: [$scope.componentsGroups]
      });
    };

    $scope.reloadData();

    let refreshPromise = $interval(() => {
      if ($scope.criteria.endTime === '0') {
        $scope.reloadData();
      }
    }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.initGraph = function() {
      $scope.nodesareachart = c3.generate({
        bindto: '#nodesareachart',
        data: {
          columns: [],
          type: 'area',
          x: 'timestamp',
          keys: {
            value: $scope.components,
            x: 'timestamp'
          },
          groups: [$scope.componentsGroups]
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
            padding: {bottom: 0},
            tick: {
              format: function (y) { return y / 1000000000; }
            }
          }
        }
      });

    };

    $scope.selectAction = function() {
      $scope.reloadData();
    };

    $scope.toggleStacked = function() {
      $scope.chartStacked = !$scope.chartStacked;
      $scope.componentsGroups = $scope.chartStacked ? $scope.components : [];
      $scope.initGraph();
      $scope.redrawAreaChart();
    };

    $scope.pauseLiveData = function() {
      $scope.criteria.endTime = $scope.criteria.endTime === '0' ? ('' + +new Date()) : '0';
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

  }]);
}
