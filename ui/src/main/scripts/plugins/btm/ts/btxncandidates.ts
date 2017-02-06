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

  export let BTMCandidatesController = _module.controller('BTM.TxnCandidatesController', ['$scope', '$http',
    '$location', '$interval', ($scope, $http, $location, $interval) => {

    $scope.newTxnName = '';
    $scope.existingTxnName = '';
    $scope.selectedendpoints = [ ];
    $scope.candidateCount = 0;

    $http.get('/hawkular/apm/config/transaction/summary').then(function(resp) {
      $scope.transactions = resp.data;
    },function(resp) {
      console.log('Failed to get transaction summaries: ' + angular.toJson(resp));
    });

    $scope.reload = function() {
      $http.get('/hawkular/apm/analytics/unboundendpoints?compress=true').then(function(resp) {
        $scope.unboundendpoints = resp.data;
        $scope.candidateCount = Object.keys(resp.data).length;

        let selected = $scope.selectedendpoints;
        $scope.selectedendpoints = [];

        for (let i = 0; i < $scope.unboundendpoints.length; i++) {
          for (let j = 0; j < selected.length; j++) {
            if ($scope.unboundendpoints[i].endpoint === selected[j].endpoint) {
              $scope.selectedendpoints.push($scope.unboundendpoints[i]);
            }
          }
        }
      },function(resp) {
        console.log('Failed to get unbound URIs: ' + angular.toJson(resp));
      });
    };

    $scope.reload();

    let refreshPromise = $interval(() => { $scope.reload(); }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.addTxn = function() {
      let defn = {
        filter: {
          inclusions: []
        },
        processors: []
      };
      for (let i = 0; i < $scope.selectedendpoints.length; i++) {
        defn.filter.inclusions.push($scope.selectedendpoints[i].regex);
        if ($scope.selectedendpoints[i].uriTemplate !== undefined) {
          defn.processors.push({
            description: 'Process inbound request',
            nodeType: 'Consumer',
            direction: 'In',
            actions: [{
              actionType: 'EvaluateURI',
              description: 'Extract parameters from path',
              template: $scope.selectedendpoints[i].uriTemplate
            }]
          });
        }
      }
      $http.put('/hawkular/apm/config/transaction/full/' + $scope.newTxnName, defn).then(function(resp) {
        $location.path('/hawkular-ui/apm/btm/config/' + $scope.newTxnName);
      },function(resp) {
        console.log('Failed to add transaction \'' + $scope.newTxnName + '\': ' + angular.toJson(resp));
      });
    };

    $scope.ignoreTxn = function() {
      let defn = {
        level: 'Ignore',
        filter: {
          inclusions: []
        },
        processors: []
      };
      for (let i = 0; i < $scope.selectedendpoints.length; i++) {
        defn.filter.inclusions.push($scope.selectedendpoints[i].regex);
        // Even though ignored, add URI evaluation in case later on we want to manage the txn
        if ($scope.selectedendpoints[i].uriTemplate !== undefined) {
          defn.processors.push({
            description: 'Process inbound request',
            nodeType: 'Consumer',
            direction: 'In',
            actions: [{
              actionType: 'EvaluateURI',
              description: 'Extract parameters from path',
              template: $scope.selectedendpoints[i].uriTemplate
            }]
          });
        }
      }
      $http.put('/hawkular/apm/config/transaction/full/' + $scope.newTxnName, defn).then(function(resp) {
        $location.path('/hawkular-ui/apm/btm/config/' + $scope.newTxnName);
      },function(resp) {
        console.log('Failed to ignore transaction \'' + $scope.newTxnName + '\': ' + angular.toJson(resp));
      });
    };

    $scope.updateTxn = function() {
      $http.get('/hawkular/apm/config/transaction/full/' + $scope.existingTxnName).then(function(resp) {
        let txn = resp.data;
        for (let i = 0; i < $scope.selectedendpoints.length; i++) {
          if (txn.filter.inclusions.indexOf($scope.selectedendpoints[i].regex) === -1) {
            txn.filter.inclusions.push($scope.selectedendpoints[i].regex);
          }
        }
        $http.put('/hawkular/apm/config/transaction/full/' + $scope.existingTxnName,txn).then(function(resp) {
          console.log('Saved updated transaction \'' + $scope.existingTxnName + '\': ' + angular.toJson(resp));
          $location.path('/hawkular-ui/apm/btm/config/' + $scope.existingTxnName);
        },function(resp) {
          console.log('Failed to save transaction \'' + $scope.existingTxnName + '\': ' + angular.toJson(resp));
        });
      },function(resp) {
        console.log('Failed to get transaction \'' + $scope.existingTxnName + '\': ' + angular.toJson(resp));
      });
    };

    $scope.selectionChanged = function(uriinfo) {
      for (let i = 0; i < $scope.selectedendpoints.length; i++) {
        if ($scope.selectedendpoints[i].endpoint === uriinfo.endpoint) {
          $scope.selectedendpoints.remove($scope.selectedendpoints[i]);
          return;
        }
      }
      $scope.selectedendpoints.push(uriinfo);
    };

    $scope.isSelected = function(uriinfo) {
      for (let i = 0; i < $scope.selectedendpoints.length; i++) {
        if ($scope.selectedendpoints[i].endpoint === uriinfo.endpoint) {
          return true;
        }
      }
      return false;
    };

    $scope.getLevel = function(level) {
      if (level === 'All') {
        return 'Active';
      }
      return level;
    };

  }]);

}
