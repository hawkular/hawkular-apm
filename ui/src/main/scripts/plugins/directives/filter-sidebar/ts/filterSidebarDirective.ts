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

/// <reference path='filterSidebarPlugin.ts'/>
module FilterSidebar {

  export class FilterSidebarDirective {

    public restrict = 'E';
    public transclude = false;
    public replace = true;

    public templateUrl = templatePath;

    private defaultCriteria;
    private theScope;

    public link: (scope, elm, attrs, ctrl) => any;

    constructor(public $compile, public $rootScope, public $http) {

      $rootScope.timeSpans = [
        {time: '',             text: 'Custom...'},
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

      this.defaultCriteria = {
        transaction: '',
        hostName: '',
        properties: [],
        startTime: $rootScope.timeSpans[4].time,
        endTime: '0'
      };

      $rootScope.sbFilter = $rootScope.sbFilter || {};
      $rootScope.sbFilter.criteria = $rootScope.sbFilter.criteria || this.defaultCriteria;
      $rootScope.sbFilter.data = $rootScope.sbFilter.data || {};
      $rootScope.sbFilter.defaultStartTime = this.defaultCriteria.startTime;

      // FIXME: this should not go into rootScope
      $rootScope.startDTOptions = {
        //format: 'DD-MM-YYYY HH:mm:ss'
      };

      $rootScope.endDTOptions = {
        //format: 'DD-MM-YYYY HH:mm:ss',
        useCurrent: false
      };

      // necessary to ensure 'this' is this object <sigh>
      this.link = (scope, elm, attrs, ctrl) => {
        return this.doLink(scope, elm, attrs, ctrl, $compile, $rootScope, $http);
      };
    }

    private doLink(scope, elm, attrs, ctrl, $compile, $rootScope, $http): void {
      this.theScope = scope;
      scope.fsb = {
        showTime: !attrs.hasOwnProperty('noTime'),
        showText: !attrs.hasOwnProperty('noText'),
        showBtxns: !attrs.hasOwnProperty('noBtxns'),
        showHosts: !attrs.hasOwnProperty('noHosts'),
        showProps: !attrs.hasOwnProperty('noProps'),
        showFaults: !attrs.hasOwnProperty('noFaults')
      };

      scope.$on('dataUpdated', () => {
        this.updateSidebarData(scope);
      });

      if (scope.fsb.showTime) {
        $rootScope.updateCriteriaTimeSpan = function() {
          if ($rootScope.sbFilter.timeSpan !== '') { // using preset
            $rootScope.sbFilter.criteria.startTime = $rootScope.sbFilter.timeSpan;
          } else {
            if ($rootScope.sbFilter.customStartTime) {
              $rootScope.sbFilter.criteria.startTime = +new Date($rootScope.sbFilter.customStartTime);
            } else {
              $rootScope.sbFilter.criteria.startTime = $rootScope.sbFilter.defaultStartTime;
            }
            if ($rootScope.sbFilter.customEndTime) {
              $rootScope.sbFilter.criteria.endTime = +new Date($rootScope.sbFilter.customEndTime);
            }
          }
        };

        $rootScope.sbFilter.timeSpan = angular.isUndefined($rootScope.sbFilter.timeSpan) ?
          this.defaultCriteria.startTime : $rootScope.sbFilter.timeSpan;
        $rootScope.$watch('sbFilter.timeSpan', (newValue, oldValue) => {
          if (oldValue !== '' && newValue === '') { // setting a custom time
            $rootScope.sbFilter.customStartTime = new Date(+new Date() + parseInt(oldValue, 10));
            $rootScope.sbFilter.customEndTime = new Date();
          } else if (oldValue === '') { // returning from custom
            $rootScope.sbFilter.criteria.endTime = '0';
          }
          $rootScope.updateCriteriaTimeSpan();
        });
      }

      if (scope.fsb.showProps) {
        $rootScope.$watch('sbFilter.selPropName', (newValue, oldValue) => {
          if (newValue && newValue !== oldValue) {
            this.updatePropertyValues(newValue);
          }
        });
      }
    }

    private updatePropertyValues = function(newValue) {
      this.theScope.sbPropertyValues = [];
      let propVal = this.$http.get('/hawkular/apm/analytics/trace/completion/property/' + newValue.name +
        '?criteria=' + encodeURI(angular.toJson(this.$rootScope.sbFilter.criteria)));
      propVal.then((resp) => {
        this.theScope.sbPropertyValues = resp.data;
      });
    };

    private updateSidebarData(scope) {
      if (scope.fsb.showBtxns) {
        this.$http.get('/hawkular/apm/analytics/transactions?criteria=' +
            encodeURI(angular.toJson(this.$rootScope.sbFilter.criteria))).then((resp) => {
          this.$rootScope.sbFilter.data.transactions = _.map(resp.data, function(o: any){ return o.name; });
        }, (error) => {
          console.log('Failed to get transactions: ' + angular.toJson(error));
        });
      }

      if (scope.fsb.showHosts) {
        this.$http.get('/hawkular/apm/analytics/hostnames?criteria=' +
            encodeURI(this.criteriaToJson(this.$rootScope.sbFilter.criteria))).then((resp) => {
          this.$rootScope.sbFilter.data.hostNames = resp.data || [];
        }, (error) => {
          console.log('Failed to get host names: ' + angular.toJson(error));
        });
      }

      if (scope.fsb.showProps) {
        this.$http.get('/hawkular/apm/analytics/properties?criteria=' +
            encodeURI(angular.toJson(this.$rootScope.sbFilter.criteria))).then((resp) => {
          this.$rootScope.sbFilter.data.properties = resp.data || [];
          if (scope.selPropName && _.find(resp.data, scope.selPropName)) {
            this.updatePropertyValues(scope.selPropName);
          }
        }, (error) => {
            console.log('Failed to get properties: ' + angular.toJson(error));
        });
      }

    }

    private criteriaToJson(criteria) {
      let txncriteria = angular.copy(criteria);
      if (txncriteria.transaction === '') {
        delete txncriteria.transaction;
      }
      return angular.toJson(txncriteria);
    }
  }
}
