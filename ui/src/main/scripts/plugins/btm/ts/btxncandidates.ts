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

  export var BTMCandidatesController = _module.controller("BTM.BTMCandidatesController", ["$scope", "$http", '$location', '$uibModal', '$interval', ($scope, $http, $location, $uibModal, $interval) => {

    $scope.newBTxnName = '';
    $scope.existingBTxnName = '';
    $scope.selecteduris = [ ];
    $scope.candidateCount = 0;

    $http.get('/hawkular/btm/config/businesstxn/summary').then(function(resp) {
      $scope.businessTransactions = resp.data;
    },function(resp) {
      console.log("Failed to get business txn summaries: "+JSON.stringify(resp));
    });

    $scope.reload = function() {
      $http.get('/hawkular/btm/analytics/unbounduris?compress=true').then(function(resp) {
        $scope.unbounduris = resp.data;
        $scope.candidateCount = Object.keys(resp.data).length;
        
        var selected = $scope.selecteduris;
        $scope.selecteduris = [];
        
        for (var i=0; i < $scope.unbounduris.length; i++) {
          for (var j=0; j < selected.length; j++) {
            if ($scope.unbounduris[i].uri === selected[j].uri) {
              $scope.selecteduris.add($scope.unbounduris[i]);
            }
          }
        }
      },function(resp) {
        console.log("Failed to get unbound URIs: "+JSON.stringify(resp));
      });
    };

    $scope.reload();

    $interval(function() {
      $scope.reload();
    },10000);

    $scope.addBusinessTxn = function() {
      var defn = {
        filter: {
          inclusions: []
        },
        processors: []
      };
      for (var i = 0; i < $scope.selecteduris.length; i++) {
        defn.filter.inclusions.add($scope.selecteduris[i].regex);
        if ($scope.selecteduris[i].template !== undefined) {
          defn.processors.add({
            description: "Process inbound request",
            nodeType: "Consumer",
            direction: "In",
            uriFilter: $scope.selecteduris[i].regex,
            actions: [{
              actionType: "EvaluateURI",
              description: "Extract parameters from path",
              template: $scope.selecteduris[i].template
            }]
          });
        }
      }
      $http.put('/hawkular/btm/config/businesstxn/full/'+$scope.newBTxnName, defn).then(function(resp) {
        $location.path('config/'+$scope.newBTxnName);
      },function(resp) {
        console.log("Failed to add business txn '"+$scope.newBTxnName+"': "+JSON.stringify(resp));
      });
    };

    $scope.ignoreBusinessTxn = function() {
      var defn = {
        level: 'Ignore',
        filter: {
          inclusions: []
        },
        processors: []
      };
      for (var i = 0; i < $scope.selecteduris.length; i++) {
        defn.filter.inclusions.add($scope.selecteduris[i].regex);
        // Even though ignored, add URI evaluation in case later on we want to manage the btxn
        if ($scope.selecteduris[i].template !== undefined) {
          defn.processors.add({
            description: "Process inbound request",
            nodeType: "Consumer",
            direction: "In",
            uriFilter: $scope.selecteduris[i].regex,
            actions: [{
              actionType: "EvaluateURI",
              description: "Extract parameters from path",
              template: $scope.selecteduris[i].template
            }]
          });
        }
      }
      $http.put('/hawkular/btm/config/businesstxn/full/'+$scope.newBTxnName, defn).then(function(resp) {
        $location.path('config/'+$scope.newBTxnName);
      },function(resp) {
        console.log("Failed to ignore business txn '"+$scope.newBTxnName+"': "+JSON.stringify(resp));
      });
    };

    $scope.updateBusinessTxn = function() {
      $http.get('/hawkular/btm/config/businesstxn/full/'+$scope.existingBTxnName).then(function(resp) {
        var btxn = resp.data;
        for (var i = 0; i < $scope.selecteduris.length; i++) {
          if (btxn.filter.inclusions.indexOf($scope.selecteduris[i].regex) === -1) {
            btxn.filter.inclusions.add($scope.selecteduris[i].regex);
          }
        }
        $http.put('/hawkular/btm/config/businesstxn/full/'+$scope.existingBTxnName,btxn).then(function(resp) {
          console.log("Saved updated business txn '"+$scope.existingBTxnName+"': "+JSON.stringify(resp));
          $location.path('config/'+$scope.existingBTxnName);
        },function(resp) {
          console.log("Failed to save business txn '"+$scope.existingBTxnName+"': "+JSON.stringify(resp));
        });
      },function(resp) {
        console.log("Failed to get business txn '"+$scope.existingBTxnName+"': "+JSON.stringify(resp));
      });
    };

    $scope.selectionChanged = function(uriinfo) {
      for (var i=0; i < $scope.selecteduris.length; i++) {
        if ($scope.selecteduris[i].uri === uriinfo.uri) {
          $scope.selecteduris.remove($scope.selecteduris[i]);
          return;
        }
      }
      $scope.selecteduris.add(uriinfo);
    };
    
    $scope.isSelected = function(uriinfo) {
      for (var i=0; i < $scope.selecteduris.length; i++) {
        if ($scope.selecteduris[i].uri === uriinfo.uri) {
          return true;
        }
      }
      return false;
    };
    
    $scope.getLevel = function(level) {
      if (level === 'All') {
        return "Active";
      }
      return level;
    };

  }]);

}

