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

/// <reference path='filterSidebarPlugin.ts'/>
module FilterSidebar {

  export class FilterSidebarDirective {

    public restrict = 'E';
    public transclude = false;
    public replace = true;

    public templateUrl = templatePath;

    public link: (scope, elm, attrs, ctrl) => any;

    constructor(public $compile, public $rootScope) {

      $rootScope.timeSpans = [
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

      let defaultCriteria = {
        businessTransaction: '',
        hostName: '',
        properties: [],
        startTime: $rootScope.timeSpans[3].time,
        endTime: '0'
      };

      $rootScope.sbFilter = $rootScope.sbFilter || {};
      $rootScope.sbFilter.criteria = $rootScope.sbFilter.criteria || defaultCriteria;

      // necessary to ensure 'this' is this object <sigh>
      this.link = (scope, elm, attrs, ctrl) => {
        return this.doLink(scope, elm, attrs, ctrl, $compile, $rootScope);
      };
    }

    private doLink(scope, elm, attrs, ctrl, $compile, $rootScope): void {
      scope.fsb = {
        showTime: !attrs.hasOwnProperty('noTime'),
        showText: !attrs.hasOwnProperty('noText'),
        showBtxns: !attrs.hasOwnProperty('noBtxns'),
        showHosts: !attrs.hasOwnProperty('noHosts')
      };
    }
  }
}
