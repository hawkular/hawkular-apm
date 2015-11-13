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

  export var BTxnInfoController = _module.controller("BTM.BTxnInfoController", ["$scope", "$routeParams", "$http", '$location', '$interval', ($scope, $routeParams, $http, $location, $interval) => {

    $scope.businessTransactionName = $routeParams.businesstransaction;

    $scope.criteria = {
      name: $scope.businessTransactionName,
      startTime: -3600000,
      endTime: 0
    };

    $scope.config = {
      interval: 60000
    };

    $scope.reload = function() {
      $http.post('/hawkular/btm/analytics/businesstxn/completion/statistics?interval='+$scope.config.interval, $scope.criteria).then(function(resp) {
        $scope.statistics = resp.data;
        
        $scope.ctlinechart.load({
          json: $scope.statistics,
          keys: {
            value: ['min','average','max','count'],
            x: 'timestamp'
          }
        });

      },function(resp) {
        console.log("Failed to get statistics: "+resp);
      });
    };

    $scope.reload();

    $interval(function() {
      $scope.reload();
    },10000);

    $scope.initGraph = function() {
      $scope.ctlinechart = c3.generate({
        bindto: '#completiontimelinechart',
        data: {
          json: [
          ],
          axes: {
            min: 'y',
            average: 'y',
            max: 'y',
            count: 'y2'
          },
          type: 'line',
          types: {
            count: 'bar'
          },
          keys: {
            value: ['min','average','max','count'],
            x: 'timestamp'
          }
        },
        color: {
          pattern: ['#e5e600', '#33cc33', '#ff0000', '#99ccff']
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
          y2: {
            show: true
          }
        }
      });

    };
    
    $scope.initGraph();

  }]);

}
