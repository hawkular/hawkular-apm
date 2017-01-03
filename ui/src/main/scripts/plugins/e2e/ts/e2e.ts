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

/// <reference path="e2ePlugin.ts"/>
module E2E {

  declare let dagreD3: any;

  export let E2EController = _module.controller('E2E.E2EController', ['$scope', '$rootScope', '$routeParams', '$http',
    '$location', '$interval', '$timeout', '$modal', ($scope, $rootScope, $routeParams, $http, $location, $interval,
    $timeout, $modal) => {

    $scope.reload = function() {

      $rootScope.updateCriteriaTimeSpan();

      let commPromise = $http.get('/hawkular/apm/analytics/communication/summary?tree=true&criteria=' +
                                   encodeURI(angular.toJson($rootScope.sbFilter.criteria)));
      commPromise.then(function(resp) {
        $scope.e2eData = resp.data;
        $scope.e2eData.sort($scope.compareNodes);
        $scope.findTopLevels();
        $scope.rootNode = _.indexOf($scope.topLevel, $scope.rootNode) > -1 ? $scope.rootNode : _.first($scope.topLevel);
        $scope.filterByTopLevel($scope.rootNode, true);
      }, function(resp) {
        console.log('Failed to get end-to-end data: ' + angular.toJson(resp));
      });

      $scope.updateInstanceCount();

      // this informs the sidebar directive, so it'll update it's data as well
      $scope.$broadcast('dataUpdated');
    };

    $scope.updateInstanceCount = function() {

      let localCriteria = JSON.parse(angular.toJson($rootScope.sbFilter.criteria));
      let nodeIndex = _.indexOf($scope.topLevel, $scope.rootNode);

      if (nodeIndex > -1) {
        localCriteria.uri = $scope.uris[nodeIndex];
        localCriteria.operation = $scope.operations[nodeIndex];
      }

      $http.get('/hawkular/apm/analytics/trace/completion/count?criteria=' + encodeURI(angular.toJson(localCriteria))).
        then((resp) => {
        $scope.instanceCount = resp.data || 0;
      }, (error) => {
        console.log('Failed to get instance count: ' + angular.toJson(error));
      });
    };

    let refreshPromise = $interval(() => { $scope.reload(); }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $rootScope.$watch('sbFilter.criteria', $scope.reload, true);

    $scope.$watch('rootNode', $scope.updateInstanceCount, true);

    // get top level nodes
    $scope.findTopLevels = function() {
      $scope.topLevel = [];
      $scope.uris = [];
      $scope.operations = [];

      _.each($scope.e2eData, (node) => {
        $scope.topLevel.push($scope.idWithServiceName(node));
        $scope.uris.push(node.uri);
        $scope.operations.push(node.operation);
      });
    };

    $scope.idWithServiceName = function (node) {
      let serviceName = '';
      if (node.serviceName) {
        serviceName = node.serviceName + ': ';
      }
      return serviceName + node.id;
    };

    $scope.compareNodes = function(a, b) {
      return $scope.idWithServiceName(a) > $scope.idWithServiceName(b);
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

    $scope.filterByTopLevel = function(nodeId) {
      if (nodeId) {
        let branch = _.find($scope.e2eData, (node: any) => {
          return $scope.idWithServiceName(node) === nodeId;
        });
        $scope.filteredNodes = branch ? [branch] : [];
      }
    };

    let ModalInstanceCtrl = function ($scope, $modalInstance, $log, rootNode, topLevel, uris, operations) {

      $scope.rootNode = {
        'uri': rootNode
      };

      let localCriteria = JSON.parse(angular.toJson($rootScope.sbFilter.criteria));
      let nodeIndex = _.indexOf(topLevel, rootNode);

      if (nodeIndex > -1) {
        localCriteria.uri = uris[nodeIndex];
        localCriteria.operation = operations[nodeIndex];
      }

      let instDetails = $http.get('/hawkular/apm/analytics/trace/completion/times?criteria=' +
                                   encodeURI(angular.toJson(localCriteria)));

      instDetails.then(function(resp) {
        _.forEach(resp.data, (datapoint: any) => {
            datapoint.timestamp = datapoint.timestamp / 1000; // Convert from micro to milliseconds
            datapoint.propertiesGrouped = [];
            _.forEach(datapoint.properties, (dpProp: any) => {
              let newProp: any = _.find(datapoint.propertiesGrouped, { 'name': dpProp.name });
              if (newProp) {
                newProp.value += ', ' + dpProp.value;
              } else {
                newProp = { 'name' : dpProp.name, 'value': dpProp.value };
                datapoint.propertiesGrouped.push(newProp);
              }
            });
        });
        $scope.timesData = resp.data;
      });

      $scope.checkMinMaxDuration = function() {
        $scope.maxDuration = Math.max($scope.maxDuration, $scope.minDuration);
      };

      // Table sorting
      $scope.sortKey = 'timestamp';
      $scope.reverse = true;

      $scope.sort = function(keyname) {
        if ($scope.sortKey === keyname) {
          $scope.reverse = !$scope.reverse; // if true make it false and vice versa
        } else {
          $scope.sortKey = keyname; // set the sortKey to the param passed
        }
      };

      $scope.durationRange = function (entry) {
        return entry.duration >= (($scope.minDuration * 1000) || 0) &&
          entry.duration <= ($scope.maxDuration === 0 ? 0 : (($scope.maxDuration * 1000) || Number.MAX_VALUE));
      };

      // Pagination
      $scope.numPerPage = 15;

      let getMaxDuration = function(nodes, maxDuration) {
        _.forEach(nodes, (node: any) => {
          maxDuration = node.duration > maxDuration ? node.duration : maxDuration;
          if(node.nodes && node.nodes.length > 0) {
            maxDuration = getMaxDuration(node.nodes, maxDuration);
          }
        });
        return maxDuration;
      };

      let calcSeverity = function(nodes, maxDuration) {
        _.forEach(nodes, (node: any) => {
          let percentage = node.duration / maxDuration;
          if (percentage >= 0.8) {
            node.severity = 4;
          } else if (percentage >= 0.6) {
            node.severity = 3;
          } else if (percentage >= 0.4) {
            node.severity = 2;
          } else if (percentage >= 0.2) {
            node.severity = 1;
          } else {
            node.severity = 0;
          }
          if(node.nodes && node.nodes.length > 0) {
            calcSeverity(node.nodes, maxDuration);
          }
        });
      };

      $scope.showIVD = function(txId) {
        if ($scope.selectedTx === txId) {
          $scope.selectedTx = '';
        } else {
          $http.get('/hawkular/apm/traces/complete/' + txId).then(function(resp) {
            calcSeverity(resp.data.nodes, getMaxDuration(resp.data.nodes, 0));
            $scope.instDetails = resp.data.nodes;
            $scope.selectedTx = txId;
          });
        }
      };

      $scope.close = function() {
        $modalInstance.dismiss('cancel');
      };
    };

    $scope.showInstanceDetails = function() {
      $modal.open({
        templateUrl: 'plugins/e2e/html/details-modal.html',
        controller: ModalInstanceCtrl,
        size: 'xl',
        resolve: {
          rootNode: function () {
            return $scope.rootNode;
          },
          topLevel: function () {
            return $scope.topLevel;
          },
          uris: function() {
            return $scope.uris;
          },
          operations: function() {
            return $scope.operations;
          }
        }
      });
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
