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

  export let E2EController = _module.controller('E2E.E2EController', ['$scope', '$rootScope', '$routeParams', '$http',
    '$location', '$interval', '$timeout', ($scope, $rootScope, $routeParams, $http, $location, $interval, $timeout) => {

    $scope.reload = function() {
      let commPromise = $http.post('/hawkular/btm/analytics/communication/summary?tree=true&startTime=' +
        ($rootScope.sbFilter ? $rootScope.sbFilter.criteria.startTime : 0), $rootScope.sbFilter.criteria);
      commPromise.then(function(resp) {
        $scope.e2eData = resp.data;
        $scope.findTopLevels();
        $scope.rootNode = $scope.rootNode || _.first($scope.topLevel);
        $scope.filterByTopLevel($scope.rootNode, true);
      }, function(resp) {
        console.log('Failed to get end-to-end data: ' + JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
        $scope.businessTransactions = _.map(resp.data, function(o: any){ return o.name; });
      },function(resp) {
        console.log('Failed to get business txn summaries: ' + JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/hostnames').then(function(resp) {
        $scope.hostNames = resp.data || [];
      },function(resp) {
        console.log('Failed to get host names: ' + JSON.stringify(resp));
      });

    };

    $scope.reload();

    let refreshPromise = $interval(() => { $scope.reload(); }, 1000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $rootScope.$watch('sbFilter.criteria', $scope.reload, true);

    // get top level nodes
    $scope.findTopLevels = function() {
      $scope.topLevel = [];
      _.each($scope.e2eData, (node) => {
        $scope.topLevel.push(node.id);
      });
    };

    $scope.filterByTopLevel = function(nodeId) {
      if (nodeId) {
        let branch = _.find($scope.e2eData, (node: any) => {
          return node.id === nodeId;
        });
        $scope.filteredNodes = branch ? [branch] : [];
      }
    };

    $scope.resetZoom = function() {
      // we just re-draw it, by re-selecting root node
      let oldRootNode = $scope.rootNode;
      $scope.rootNode = '';
      $timeout(() => {
        $scope.rootNode = oldRootNode;
      });
    };

  }]);

}
