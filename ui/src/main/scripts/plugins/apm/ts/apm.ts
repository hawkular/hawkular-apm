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

  declare var c3: any;

  export var APMController = _module.controller("APM.APMController", ["$scope", "$routeParams", "$http", '$location', '$interval', ($scope, $routeParams, $http, $location, $interval) => {

    $scope.criteria = {
      businessTransaction: undefined,
      hostName: $routeParams.hostName,
      properties: [],
      startTime: -3600000,
      endTime: "0"
    };

    $scope.config = {
      interval: 60000,
      maxRows: 10
    };
    
    $scope.nodeComponents = [];
    
    $scope.businessTransactions = [];
    
    $scope.components = [];

    $scope.timestamps = ['timestamp'];
        
    $scope.lists = {};
        
    $scope.businessTransactions = [];
    
    $scope.reloadData = function() {

      $http.post('/hawkular/btm/analytics/node/statistics?interval='+$scope.config.interval, $scope.criteria).then(function(resp) {
        $scope.statistics = resp.data;
        
        for (var i=0; i < $scope.statistics.length; i++) {
          
          if (i === 0) {
            $scope.timestamps.length = 0;
            $scope.timestamps.push('timestamp');
          }
          $scope.timestamps.push($scope.statistics[i].timestamp);
          
          $scope.nodeComponents.push($scope.timestamps);
          
          var keys=Object.keys($scope.statistics[i].componentTypes);

          for (var j=0; j < keys.length; j++) {
            var list=$scope.lists[keys[j]];
            
            if (list === undefined) {
              list = [keys[j]];
              $scope.lists[keys[j]] = list;
              $scope.nodeComponents.push(list);
              $scope.components.push(keys[j]);
            } else if (i === 0) {
              // Clear out list
              list.length = 0;
              list.push(keys[j]);
            }
            
            for (var k=list.length; k < $scope.timestamps.length-1; k++) {
              list.push(0);
            }
            
            list.push($scope.statistics[i].componentTypes[keys[j]].duration);
          }
        }

        $scope.redrawAreaChart();
      },function(resp) {
        console.log("Failed to get node timeseries statistics: "+JSON.stringify(resp));
      });

      $http.post('/hawkular/btm/analytics/node/summary', $scope.criteria).then(function(resp) {
        $scope.summaries = resp.data;
        
        $scope.max = 0;

        for (var i=0; i < $scope.summaries.length; i++) {
          if ($scope.summaries[i].elapsed > $scope.max) {
            $scope.max = $scope.summaries[i].elapsed;
          }
        }
      },function(resp) {
        console.log("Failed to get node summary statistics: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
        $scope.businessTransactions.length = 0;

        for (var i=0; i < resp.data.length; i++) {
          $scope.businessTransactions.add(resp.data[i].name);
        }
      },function(resp) {
        console.log("Failed to get business txn summaries: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/hostnames').then(function(resp) {
        $scope.hostNames = [ ];
        for (var i=0; i < resp.data.length; i++) {
          $scope.hostNames.add(resp.data[i]);
        }
      },function(resp) {
        console.log("Failed to get host names: "+JSON.stringify(resp));
      });
    };

    $scope.redrawAreaChart = function() {
      $scope.nodesareachart.load({
        columns: $scope.nodeComponents,
        keys: {
          value: $scope.components,
          x: 'timestamp'
        },
        groups: [$scope.components]
      });
    };

    $scope.reloadData();

    $interval(function() {
      if ($scope.criteria.endTime === "0") {
        $scope.reloadData();
      }    
    },10000);

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
          groups: [$scope.components]
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
    
    $scope.initGraph();

    $scope.selectAction = function() {
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
