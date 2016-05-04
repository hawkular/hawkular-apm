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

/// <reference path="e2ePlugin.ts"/>
module E2E {

  declare let dagreD3: any;

  export let E2EController = _module.controller('E2E.E2EController', ['$scope', '$routeParams', '$http', '$location',
    '$interval', '$timeout', ($scope, $routeParams, $http, $location, $interval, $timeout) => {

    // some sample data while we don't have an actual endpoint
    $scope.sampleData =
    [
    {
      'averageDuration': 342,
      'count': 1,
      'id': '/items[POST]',
      'maximumDuration': 342,
      'minimumDuration': 342,
      'outbound': {}
    },
    {
      'averageDuration': 375,
      'count': 2,
      'id': '/search[GET]',
      'maximumDuration': 734,
      'minimumDuration': 17,
      'outbound': {}
    },
    {
      'averageDuration': 2296,
      'count': 2,
      'id': '/book[GET]',
      'maximumDuration': 3527,
      'minimumDuration': 1065,
      'outbound': {
        '/book/14293[GET]': {
          'averageLatency': 130,
          'count': 2,
          'maximumLatency': 256,
          'minimumLatency': 4
        }
      }
    },
    {
      'averageDuration': 275,
      'count': 2,
      'id': '/book/14293[GET]',
      'maximumDuration': 548,
      'minimumDuration': 2,
      'outbound': {}
    },
    {
      'averageDuration': 2979,
      'count': 2,
      'id': '/items[GET]',
      'maximumDuration': 4203,
      'minimumDuration': 1755,
      'outbound': {
        '/book[GET]': {
          'averageLatency': 121,
          'count': 1,
          'maximumLatency': 121,
          'minimumLatency': 121
        }
      }
    }
    $scope.timeSpans = [
      {time: '-60000',       text: '1 Minute'},
      {time: '-600000',      text: '10 Minutes'},
      {time: '-1800000',     text: '30 Minutes'},
      {time: '-3600000',     text: '1 Hour'},
      {time: '-14400000',    text: '4 Hours'},
      {time: '-28800000',    text: '8 Hours'},
      {time: '-43200000',    text: '12 Hours'},
      {time: '-86400000',    text: 'Day'},
      {time: '-604800000',   text: 'Week'},
      {time: '-2419200000',  text: 'Month'},
      {time: '-15768000000', text: '6 Months'},
      {time: '-31536000000', text: 'Year'},
      {time: '1',            text: 'All'}
    ];

    $scope.criteria = {
      startTime: $scope.timeSpans[9].time
    };
    // get top level nodes
    $scope.findTopLevels = function() {
      $scope.topLevel = [];
      $scope.outbounds = [];
      $scope.reverseInbounds = [];
      _.each($scope.sampleData, (node) => {
        $scope.topLevel.push(node.id);
        let outbounds = Object.keys(node.outbound);
        $scope.outbounds = _.union($scope.outbounds, outbounds);
      });
      $scope.topLevel = _.xor($scope.topLevel, $scope.outbounds);
    };

    let doFilter = function(nodeId, clear) {
      if (clear) {
        $scope.allNodes = angular.copy($scope.sampleData);
        $scope.filteredNodes = [];
      }
      let filtered = _.remove($scope.allNodes, (node: any) => {
        return node.id === nodeId;
      });
      $scope.filteredNodes.push(filtered[0]);
      _.each(filtered, (node: any) => {
        _.each(Object.keys(node.outbound), (outbound) => {
          doFilter(outbound, false);
        });
      });
    };

    $scope.filterByTopLevel = function(nodeId, clear) {
      doFilter(nodeId, true);
    };

    $scope.resetZoom = function() {
      // we just re-draw it..
      $scope.filterByTopLevel($scope.rootNode, true);
    };

    $scope.findTopLevels();

  }]);

}
