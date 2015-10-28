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

  export var BTxnConfigController = _module.controller("BTM.BTxnConfigController", ["$scope", "$routeParams", "$http", '$location', ($scope, $routeParams, $http, $location) => {

    $scope.businessTransactionName = $routeParams.businesstransaction;
    $scope.dirty = false;

    $scope.newInclusionFilter = '';
    $scope.newExclusionFilter = '';

    $http.get('/hawkular/btm/config/businesstxn/'+$scope.businessTransactionName).success(function(data) {
      $scope.businessTransaction = data;
      $scope.original = angular.copy($scope.businessTransaction);
    });

    $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').success(function(data) {
      $scope.unboundURIs = data;
    });

    $scope.addInclusionFilter = function() {
      console.log('Add inclusion filter: '+$scope.newInclusionFilter);
      if ($scope.businessTransaction.filter === null) {
        $scope.businessTransaction.filter = {
          inclusions: [],
          exclusions: []
        };
      }
      $scope.businessTransaction.filter.inclusions.add($scope.newInclusionFilter);
      $scope.setDirty();
      $scope.newInclusionFilter = '';
    };

    $scope.removeInclusionFilter = function(inclusion) {
      $scope.businessTransaction.filter.inclusions.remove(inclusion);
      $scope.setDirty();
    };

    $scope.addExclusionFilter = function() {
      console.log('Add exclusion filter: '+$scope.newExclusionFilter);
      if ($scope.businessTransaction.filter === null) {
        $scope.businessTransaction.filter = {
          inclusions: [],
          exclusions: []
        };
      }
      $scope.businessTransaction.filter.exclusions.add($scope.newExclusionFilter);
      $scope.setDirty();
      $scope.newExclusionFilter = '';
    };

    $scope.removeExclusionFilter = function(exclusion) {
      $scope.businessTransaction.filter.exclusions.remove(exclusion);
      $scope.setDirty();
    };

    $scope.getExpressionText = function(expression) {
      if (expression === undefined) {
        return "";
      }
      if (expression.type === "XML") {
        return expression.source + "[" + expression.key + "]" + " xpath=" + expression.xpath;
      }
      if (expression.type === "JSON") {
        return expression.source + "[" + expression.key + "]" + " jsonpath=" + expression.jsonpath;
      }
      if (expression.type === "Text") {
        return expression.source + "[" + expression.key + "]";
      }
      return "Unknown expression type";
    };

    $scope.changedExpressionType = function(expression) {
      $scope.setDirty();
      expression.key = undefined;
      expression.source = undefined;
      expression.xpath = undefined;
      expression.jsonpath = undefined;
    };

    $scope.changedActionType = function(action) {
      $scope.setDirty();
      action.name = undefined;
      action.type = undefined;
      action.scope = undefined;
      action.predicate = undefined;
      action.expression = undefined;
    };

    $scope.addProcessor = function() {
      $scope.setDirty();
      $scope.businessTransaction.processors.add({
        description: "Processor " + ($scope.businessTransaction.processors.length + 1),
        nodeType: "Consumer",
        direction: "In",
        actions: []
      });
    };

    $scope.deleteProcessor = function(processor) {
      if (confirm('Are you sure you want to delete the processor?')) {
        $scope.setDirty();
        $scope.businessTransaction.processors.remove(processor);
      }
    };

    $scope.addAction = function(processor) {
      $scope.setDirty();
      processor.actions.add({
        description: "Action " + (processor.actions.length + 1)
      });
    };

    $scope.deleteAction = function(processor,action) {
      if (confirm('Are you sure you want to delete the action?')) {
        $scope.setDirty();
        processor.actions.remove(action);
      }
    };

    $scope.setDirty = function() {
      $scope.dirty = true;
    };

    $scope.reset = function() {
      $scope.businessTransaction = angular.copy($scope.original);
      $scope.dirty = false;
    };

    $scope.save = function() {
      $http.put('/hawkular/btm/config/businesstxn/'+$scope.businessTransactionName,$scope.businessTransaction).success(function(data) {
        $scope.original = angular.copy($scope.businessTransaction);
        $scope.dirty = false;
      });
    };

    $http.get('/hawkular/btm/config/businesstxn/'+$scope.businessTransactionName).success(function(data) {
      $scope.businessTransaction = data;
      $scope.original = angular.copy($scope.businessTransaction);
    });

  }]);

}
