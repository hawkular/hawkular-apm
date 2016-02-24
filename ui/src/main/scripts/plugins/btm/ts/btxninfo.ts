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

  declare var c3: any;

  export var BTxnInfoController = _module.controller("BTM.BTxnInfoController", ["$scope", "$routeParams", "$http", '$location', '$interval', 'btxn', ($scope, $routeParams, $http, $location, $interval, btxn) => {

    $scope.btxn = btxn;
    $scope.businessTransactionName = $routeParams.businesstransaction;

    $scope.properties = [];

    $scope.propertyValues = [];
    $scope.faultValues = [];

    $scope.criteria = {
      businessTransaction: $scope.businessTransactionName,
      properties: [],
      faults: [],
      startTime: '-3600000',
      endTime: '0',
      lowerBound: 0
    };

    $scope.config = {
      interval: '60000',
      selectedProperty: undefined,
      lowerBoundDisplay: 0,
      prevLowerBoundDisplay: 0,
      maxPropertyValues: 20,
      maxFaultValues: 20
    };

    $scope.reload = function() {
      $http.post('/hawkular/btm/analytics/completion/statistics?interval='+$scope.config.interval, $scope.criteria).then(function(resp) {
        $scope.statistics = resp.data;
        $scope.updatedBounds();
        $scope.redrawLineChart();
      },function(resp) {
        console.log("Failed to get statistics: "+JSON.stringify(resp));
      });

      var faultCriteria = angular.copy($scope.criteria);
      faultCriteria.maxResponseSize = $scope.config.maxFaultValues;

      $http.post('/hawkular/btm/analytics/completion/faults', faultCriteria).then(function(resp) {
        $scope.faults = resp.data;

        var removeFaultValues = angular.copy($scope.faultValues);

        var faultdata = [];

        for (var i=0; i < $scope.faults.length; i++) {
          var fault = $scope.faults[i];
          var record=[ ];
          record.push(fault.value);
          record.push(fault.count);
          faultdata.push(record);

          if ($scope.faultValues.indexOf(fault.value) !== -1) {
            removeFaultValues.remove(fault.value);
          } else {
            $scope.faultValues.add(fault.value);
          }
        }

        $scope.ctfaultschart.load({
          columns: faultdata
        });

        for (var j=0; j < removeFaultValues.length; j++) {
          $scope.ctfaultschart.unload(removeFaultValues[j]);
          $scope.faultValues.remove(removeFaultValues[j]);
        }

      },function(resp) {
        console.log("Failed to get statistics: "+JSON.stringify(resp));
      });

      $http.get('/hawkular/btm/analytics/properties/'+$scope.businessTransactionName).then(function(resp) {
        $scope.properties = resp.data;
      },function(resp) {
        console.log("Failed to get property info: "+JSON.stringify(resp));
      });

      if ($scope.config.selectedProperty !== undefined) {
        $scope.reloadProperty();
      }
    };

    $scope.redrawLineChart = function() {
      $scope.ctlinechart.load({
        json: $scope.statistics,
        keys: {
          value: ['max','average','min','count','faultCount'],
          x: 'timestamp'
        }
      });
    };

    $scope.reloadProperty = function() {
      var propertyCriteria = angular.copy($scope.criteria);
      propertyCriteria.maxResponseSize = $scope.config.maxPropertyValues;

      $http.post('/hawkular/btm/analytics/completion/property/'+$scope.config.selectedProperty, propertyCriteria).then(function(resp) {
        $scope.propertyDetails = resp.data;

        var removePropertyValues = angular.copy($scope.propertyValues);

        var propertydata = [];

        for (var i=0; i < $scope.propertyDetails.length; i++) {
          var prop = $scope.propertyDetails[i];
          var record=[ ];
          record.push(prop.value);
          record.push(prop.count);
          propertydata.push(record);

          if ($scope.propertyValues.indexOf(prop.value) !== -1) {
            removePropertyValues.remove(prop.value);
          } else {
            $scope.propertyValues.add(prop.value);
          }
        }

        $scope.propertychart.load({
          columns: propertydata
        });

        for (var j=0; j < removePropertyValues.length; j++) {
          $scope.propertychart.unload(removePropertyValues[j]);
          $scope.propertyValues.remove(removePropertyValues[j]);
        }

      },function(resp) {
        console.log("Failed to get property details for '"+$scope.config.selectedProperty+"': "+JSON.stringify(resp));
      });
    };

    $scope.reload();

    var refreshPromise = $interval(() => {
      if ($scope.criteria.endTime === "0" || $scope.config.prevLowerBoundDisplay !== $scope.config.lowerBoundDisplay) {
        $scope.reload();

        $scope.config.prevLowerBoundDisplay = $scope.config.lowerBoundDisplay;
      }
    }, 10000);
    $scope.$on('$destroy', () => { $interval.cancel(refreshPromise); });

    $scope.initGraph = function() {
      $scope.ctlinechart = c3.generate({
        bindto: '#completiontimelinechart',
        data: {
          json: [
          ],
          axes: {
            max: 'y',
            average: 'y',
            min: 'y',
            count: 'y2',
            faultCount: 'y2'
          },
          type: 'line',
          types: {
            count: 'bar',
            faultCount: 'bar'
          },
          keys: {
            value: ['max','average','min','count','faultCount'],
            x: 'timestamp'
          }
        },
        color: {
          pattern: ['#ff0000', '#33cc33', '#e5e600', '#99ccff', '#ffb3b3']
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
          y: {
            label: 'Seconds',
            padding: {bottom: 0},
            tick: {
              format: function (y) { return y / 1000000000; }
            }
          },
          y2: {
            show: true
          }
        }
      });

      $scope.ctfaultschart = c3.generate({
        bindto: '#completiontimefaultschart',
        data: {
          json: [
          ],
          type: 'pie',
          onclick: function (d, i) {
            var fault = {
              value: d.id
            };
            $scope.criteria.faults.add(fault);
            $scope.reload();
          }
        }
      });

    };

    $scope.initGraph();

    $scope.propertyClicked = function() {
      $scope.initPropertyGraph($scope.config.selectedProperty);
    };

    $scope.initPropertyGraph = function(name) {
      $scope.propertychart = c3.generate({
        bindto: '#completiontimepropertychart',
        data: {
          columns: [
          ],
          type: 'pie',
          onclick: function (d, i) {
            var property = {
              name: name,
              value: d.id
            };
            $scope.criteria.properties.add(property);
            $scope.reload();
          }
        }
      });

      $scope.reloadProperty();
    };

    $scope.removeProperty = function(property) {
      $scope.criteria.properties.remove(property);
      $scope.reload();
    };

    $scope.removeFault = function(fault) {
      $scope.criteria.faults.remove(fault);
      $scope.reload();
    };

    $scope.toggleExclusion = function(element) {
      element.excluded = !element.excluded;
      $scope.reload();
    };

    $scope.updatedBounds = function() {
      if ($scope.config.lowerBoundDisplay === 0) {
        $scope.criteria.lowerBound = 0;
      } else {
        var maxDuration = 0;
        for (var i=0; i < $scope.statistics.length; i++) {
          if ($scope.statistics[i].max > maxDuration) {
            maxDuration = $scope.statistics[i].max;
          }
        }
        if (maxDuration > 0) {
          $scope.criteria.lowerBound = ( $scope.config.lowerBoundDisplay * maxDuration ) / 100;
        }
      }
    };

    $scope.selectAction = function() {
      $scope.reload();
    };

    $scope.pauseLiveData = function() {
      $scope.criteria.endTime = $scope.criteria.endTime === '0' ? ('' + +new Date()) : '0';
      $scope.reload();
    };

    $scope.currentDateTime = function() {
      return new Date();
    };

  }]);
}
