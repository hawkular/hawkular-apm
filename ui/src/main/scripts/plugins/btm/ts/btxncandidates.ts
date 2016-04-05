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

  export let BTMCandidatesController = _module.controller('BTM.BTMCandidatesController', ['$scope', '$http',
    '$location', '$interval', ($scope, $http, $location, $interval) => {

    $scope.newBTxnName = '';
    $scope.existingBTxnName = '';
    $scope.selectedendpoints = [ ];
    $scope.candidateCount = 0;

    $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
      $scope.businessTransactions = resp.data;
    },function(resp) {
      console.log('Failed to get business txn summaries: ' + JSON.stringify(resp));
    });

    $scope.reload = function() {
      $http.get('/hawkular/btm/analytics/unboundendpoints?compress=true').then(function(resp) {
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
        console.log('Failed to get unbound URIs: ' + JSON.stringify(resp));
      });
    };

    $scope.reload();

    let refreshPromise = $interval(() => { $scope.reload(); }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.addBusinessTxn = function() {
      let defn = {
        filter: {
          inclusions: []
        },
        processors: []
      };
      for (let i = 0; i < $scope.selectedendpoints.length; i++) {
        defn.filter.inclusions.push($scope.selectedendpoints[i].regex);
        if ($scope.selectedendpoints[i].template !== undefined) {
          defn.processors.push({
            description: 'Process inbound request',
            nodeType: 'Consumer',
            direction: 'In',
            actions: [{
              actionType: 'EvaluateURI',
              description: 'Extract parameters from path',
              template: $scope.selectedendpoints[i].template
            }]
          });
        }
      }
      $http.put('/hawkular/btm/config/businesstxn/full/' + $scope.newBTxnName, defn).then(function(resp) {
        $location.path('/hawkular-ui/btm/config/' + $scope.newBTxnName);
      },function(resp) {
        console.log('Failed to add business txn \'' + $scope.newBTxnName + '\': ' + JSON.stringify(resp));
      });
    };

    $scope.ignoreBusinessTxn = function() {
      let defn = {
        level: 'Ignore',
        filter: {
          inclusions: []
        },
        processors: []
      };
      for (let i = 0; i < $scope.selectedendpoints.length; i++) {
        defn.filter.inclusions.push($scope.selectedendpoints[i].regex);
        // Even though ignored, add URI evaluation in case later on we want to manage the btxn
        if ($scope.selectedendpoints[i].template !== undefined) {
          defn.processors.push({
            description: 'Process inbound request',
            nodeType: 'Consumer',
            direction: 'In',
            actions: [{
              actionType: 'EvaluateURI',
              description: 'Extract parameters from path',
              template: $scope.selectedendpoints[i].template
            }]
          });
        }
      }
      $http.put('/hawkular/btm/config/businesstxn/full/' + $scope.newBTxnName, defn).then(function(resp) {
        $location.path('/hawkular-ui/btm/config/' + $scope.newBTxnName);
      },function(resp) {
        console.log('Failed to ignore business txn \'' + $scope.newBTxnName + '\': ' + JSON.stringify(resp));
      });
    };

    $scope.updateBusinessTxn = function() {
      $http.get('/hawkular/btm/config/businesstxn/full/' + $scope.existingBTxnName).then(function(resp) {
        let btxn = resp.data;
        for (let i = 0; i < $scope.selectedendpoints.length; i++) {
          if (btxn.filter.inclusions.indexOf($scope.selectedendpoints[i].regex) === -1) {
            btxn.filter.inclusions.push($scope.selectedendpoints[i].regex);
          }
        }
        $http.put('/hawkular/btm/config/businesstxn/full/' + $scope.existingBTxnName,btxn).then(function(resp) {
          console.log('Saved updated business txn \'' + $scope.existingBTxnName + '\': ' + JSON.stringify(resp));
          $location.path('/hawkular-ui/btm/config/' + $scope.existingBTxnName);
        },function(resp) {
          console.log('Failed to save business txn \'' + $scope.existingBTxnName + '\': ' + JSON.stringify(resp));
        });
      },function(resp) {
        console.log('Failed to get business txn \'' + $scope.existingBTxnName + '\': ' + JSON.stringify(resp));
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

