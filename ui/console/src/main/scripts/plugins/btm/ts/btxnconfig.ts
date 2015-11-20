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

  export var BTxnConfigController = _module.controller("BTM.BTxnConfigController", ["$scope", "$routeParams", "$http", '$location', '$interval', ($scope, $routeParams, $http, $location, $interval) => {

    $scope.businessTransactionName = $routeParams.businesstransaction;
    $scope.dirty = false;

    $scope.newInclusionFilter = '';
    $scope.newExclusionFilter = '';

    $scope.messages = [];

    $http.get('/hawkular/btm/config/businesstxn/'+$scope.businessTransactionName).then(function(resp) {
      $scope.businessTransaction = resp.data;
      $scope.original = angular.copy($scope.businessTransaction);

      $http.post('/hawkular/btm/config/businesstxn/validate',$scope.businessTransaction).then(function(resp) {
        $scope.messages = resp.data;
      },function(resp) {
        console.log("Failed to validate business txn '"+$scope.businessTransactionName+"': "+JSON.stringify(resp));
      });
    },function(resp) {
      console.log("Failed to get business txn '"+$scope.businessTransactionName+"': "+JSON.stringify(resp));
    });

    $http.get('/hawkular/btm/analytics/businesstxn/unbounduris').then(function(resp) {
      $scope.unboundURIs = [ ];
      for (var i=0; i < resp.data.length; i++) {
        var regex = $scope.escapeRegExp(resp.data[i].uri);
        if (regex !== undefined) {
          $scope.unboundURIs.add(regex);
        }
      }
    },function(resp) {
      console.log("Failed to get unbound URIs: "+resp);
    });

    $scope.reload = function() {
      $http.get('/hawkular/btm/analytics/businesstxn/bounduris/'+$scope.businessTransactionName).then(function(resp) {
        $scope.boundURIs = [ ];
        for (var i = 0; i < resp.data.length; i++) {
          var regex = $scope.escapeRegExp(resp.data[i]);
          $scope.boundURIs.add(regex);
        }
      },function(resp) {
        console.log("Failed to get bound URIs for business txn '"+$scope.businessTransactionName+"': "+JSON.stringify(resp));
      });
    };

    $scope.reload();

    $interval(function() {
      $scope.reload();
    },10000);

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

      if (expression.type === 'XML' || expression.type === 'JSON' || expression.type === 'Text') {
        expression.key = '0';
        expression.source = 'Content';
      }
    };

    $scope.changedActionType = function(action) {
      $scope.setDirty();
      action.name = undefined;
      action.type = undefined;
      action.scope = undefined;
      action.template = undefined;
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

    $scope.addAction = function(processor, type) {
      $scope.setDirty();
      processor.actions.add({
        actionType: type,
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
      $http.put('/hawkular/btm/config/businesstxn/'+$scope.businessTransactionName,$scope.businessTransaction).then(function(resp) {
        $scope.messages = resp.data;
        $scope.original = angular.copy($scope.businessTransaction);
        $scope.dirty = false;
      },function(resp) {
        console.log("Failed to save business txn '"+$scope.businessTransactionName+"': "+JSON.stringify(resp));
      });
    };

    $http.get('/hawkular/btm/config/businesstxn/'+$scope.businessTransactionName).then(function(resp) {
      $scope.businessTransaction = resp.data;
      $scope.original = angular.copy($scope.businessTransaction);
    },function(resp) {
      console.log("Failed to get business txn '"+$scope.businessTransactionName+"': "+resp);
    });

    $scope.escapeRegExp = function(str) {
      if (str === undefined) {
        return;
      }
      return "^" + str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&") + "$";
    };

    $scope.closeMessage = function(index) {
      $scope.messages.splice(index, 1);
    };

    $scope.getMessageType = function(entry) {
      var type = 'danger';
      if (entry.severity === 'Warning') {
        type = 'warning';
      } else if (entry.severity === 'Info') {
        type = 'success';
      }
      return type;
    };
    
    $scope.getMessageText = function(entry) {
      var message = "";
      if (entry.processor !== undefined) {
        message = "[" + entry.processor;
        
        if (entry.action !== undefined) {
          message = message + "/" + entry.action;
        }
        
        message = message + "] ";
      }
      
      message = message + entry.message;

      return message;
    };

    $scope.isError = function(processor,action,field) {
      for (var i = 0; i < $scope.messages.length; i++) {
        if ($scope.messages[i].processor === processor.description
            && $scope.messages[i].action === action.description
            && $scope.messages[i].field === field) {
          return true;
        }
      }
      return false;
    };

  }]);

}
