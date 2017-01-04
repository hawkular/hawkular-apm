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

    export let TxnConfigController = _module.controller('BTM.TxnConfigController', ['$scope', '$routeParams',
      '$http', '$location', '$interval', 'txn', ($scope, $routeParams, $http, $location, $interval, txn) => {

    $scope.txn = txn;

    $scope.transactionName = $routeParams.transaction;
    $scope.dirty = false;

    $scope.newInclusionFilter = '';
    $scope.newExclusionFilter = '';

    $scope.messages = [];

    $http.get('/hawkular/apm/config/transaction/full/' + $scope.transactionName).then(function(resp) {
      $scope.transaction = resp.data;
      $scope.original = angular.copy($scope.transaction);

      $http.post('/hawkular/apm/config/transaction/validate',$scope.transaction).then(function(resp) {
        $scope.messages = resp.data;
      },function(resp) {
        console.log('Failed to validate transaction \'' + $scope.transactionName + '\': ' +
          angular.toJson(resp));
      });
    },function(resp) {
      console.log('Failed to get transaction \'' + $scope.transactionName + '\': ' + angular.toJson(resp));
    });

    $http.get('/hawkular/apm/analytics/unboundendpoints?compress=true').then(function(resp) {
      $scope.unboundEndpoints = [ ];
      for (let i = 0; i < resp.data.length; i++) {
        if (resp.data[i].regex !== undefined) {
          $scope.unboundEndpoints.add(resp.data[i].regex);
        }
      }
    },function(resp) {
      console.log('Failed to get unbound URIs: ' + angular.toJson(resp));
    });

    $scope.reload = function() {
      $http.get('/hawkular/apm/analytics/boundendpoints/' + $scope.transactionName).then(function(resp) {
        $scope.boundEndpoints = [ ];
        for (let i = 0; i < resp.data.length; i++) {
          if (resp.data[i].uriRegex !== undefined) {
            $scope.boundEndpoints.add(resp.data[i].uriRegex);
          }
        }
      },function(resp) {
        console.log('Failed to get bound URIs for transaction \'' + $scope.transactionName + '\': ' +
          angular.toJson(resp));
      });
    };

    $scope.reload();

    let refreshPromise = $interval(() => { $scope.reload(); }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.addInclusionFilter = function() {
      console.log('Add inclusion filter: ' + $scope.newInclusionFilter);
      if ($scope.transaction.filter === null) {
        $scope.transaction.filter = {
          inclusions: [],
          exclusions: []
        };
      }
      $scope.transaction.filter.inclusions.add($scope.newInclusionFilter);
      $scope.setDirty();
      $scope.newInclusionFilter = '';
    };

    $scope.removeInclusionFilter = function(inclusion) {
      $scope.transaction.filter.inclusions.remove(inclusion);
      $scope.setDirty();
    };

    $scope.addExclusionFilter = function() {
      console.log('Add exclusion filter: ' + $scope.newExclusionFilter);
      if ($scope.transaction.filter === null) {
        $scope.transaction.filter = {
          inclusions: [],
          exclusions: []
        };
      }
      $scope.transaction.filter.exclusions.add($scope.newExclusionFilter);
      $scope.setDirty();
      $scope.newExclusionFilter = '';
    };

    $scope.removeExclusionFilter = function(exclusion) {
      $scope.transaction.filter.exclusions.remove(exclusion);
      $scope.setDirty();
    };

    $scope.getExpressionText = function(expression) {
      if (expression === undefined) {
        return '';
      }
      if (expression.type === 'XML') {
        return expression.source + '[' + expression.key + ']' + ' xpath=' + expression.xpath;
      }
      if (expression.type === 'JSON') {
        return expression.source + '[' + expression.key + ']' + ' jsonpath=' + expression.jsonpath;
      }
      if (expression.type === 'Text') {
        return expression.source + '[' + expression.key + ']';
      }
      return 'Unknown expression type';
    };

    $scope.changedExpressionType = function(parent,field,expression) {
      $scope.setDirty();
      expression.key = undefined;
      expression.source = undefined;
      expression.xpath = undefined;
      expression.jsonpath = undefined;

      if (expression.type === 'XML' || expression.type === 'JSON' || expression.type === 'Text') {
        expression.key = '0';
        expression.source = 'Content';
      } else if (expression.type === '') {
        parent[field] = undefined;
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
      $scope.transaction.processors.add({
        description: 'Processor ' + ($scope.transaction.processors.length + 1),
        nodeType: 'Consumer',
        direction: 'In',
        actions: []
      });
    };

    $scope.deleteProcessor = function(processor) {
      if (confirm('Are you sure you want to delete the processor?')) {
        $scope.setDirty();
        $scope.transaction.processors.remove(processor);
      }
    };

    $scope.addAction = function(processor, type) {
      $scope.setDirty();

      let newAction = {
        actionType: type,
        description: 'Action ' + (processor.actions.length + 1)
      };

      if (type === 'AddCorrelationId') {
        newAction['scope'] = 'Global';
      }

      processor.actions.add(newAction);
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
      $scope.transaction = angular.copy($scope.original);
      $scope.dirty = false;
    };

    $scope.save = function() {
      $scope.messages = [];

      $http.put('/hawkular/apm/config/transaction/full/' + $scope.transactionName, $scope.transaction)
        .then(function(resp) {
        $scope.messages = resp.data;
        $scope.original = angular.copy($scope.transaction);
        $scope.dirty = false;
      },function(resp) {
        console.log('Failed to save transaction \'' + $scope.transactionName + '\': ' + angular.toJson(resp));
        let message = {
          severity: Error,
          message: 'Failed to save \'' + $scope.transactionName + '\'',
          details: angular.toJson(resp.data)
        };
        $scope.messages.add(message);
      });
    };

    $http.get('/hawkular/apm/config/transaction/full/' + $scope.transactionName).then(function(resp) {
      $scope.transaction = resp.data;
      $scope.original = angular.copy($scope.transaction);
    },function(resp) {
      console.log('Failed to get transaction \'' + $scope.transactionName + '\': ' + angular.toJson(resp));
    });

    $scope.closeMessage = function(index) {
      $scope.messages.splice(index, 1);
    };

    $scope.getMessageType = function(entry) {
      let type = 'danger';
      if (entry.severity === 'Warning') {
        type = 'warning';
      } else if (entry.severity === 'Info') {
        type = 'success';
      }
      return type;
    };

    $scope.getMessageText = function(entry) {
      let message = '';
      if (entry.processor !== undefined) {
        message = '[' + entry.processor;

        if (entry.action !== undefined) {
          message = message + '/' + entry.action;
        }

        message = message + '] ';
      }

      message = message + entry.message;

      return message;
    };

    $scope.isError = function(processor,action,field) {
      for (let i = 0; i < $scope.messages.length; i++) {
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
