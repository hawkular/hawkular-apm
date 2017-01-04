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

/// <reference path="../../includes.ts"/>
/// <reference path="e2eGlobals.ts"/>
module E2E {

  export var _module = angular.module(E2E.pluginName, ['ui.bootstrap', 'patternfly.select']);

  _module.config(['$locationProvider', '$routeProvider', 'HawtioNavBuilderProvider',
    ($locationProvider, $routeProvider: ng.route.IRouteProvider, builder: HawtioMainNav.BuilderFactory) => {
    // Doesn't link an empty block
  }]);

  _module.filter('hkDuration', function (limitToFilter, numberFilter) {
    let milliRange =  1000;
    let secondRange = 1000 * milliRange;
    let minuteRange =   60 * secondRange;
    let hourRange =     60 * minuteRange;
    let dayRange =      24 * hourRange;
    let weekRange =      7 * dayRange;
    let yearRange =    365 * dayRange;

    return function (input, hideSecondary) {
      if (!isNaN(input)) {
        if (input > yearRange) {
          let weeks = input / weekRange;
          return Math.floor(weeks / 52) + 'y ' +
            (hideSecondary ? '' : numberFilter(Math.floor(weeks % 52), 0) + 'w');
        } else if (input > weekRange) {
          let days = input / dayRange;
          return Math.floor(days / 7) + 'w ' +
            (hideSecondary ? '' : numberFilter(Math.floor(days % 7), 0) + 'd');
        } else if (input > dayRange) {
          let hours = input / hourRange;
          return Math.floor(hours / 24) + 'd ' +
            (hideSecondary ? '' : numberFilter(Math.floor(hours % 24), 0) + 'h');
        } else if (input > hourRange) {
          let minutes = input / minuteRange;
          return Math.floor(minutes / 60) + 'h ' +
            (hideSecondary ? '' : numberFilter(Math.floor(minutes % 60), 0) + 'min');
        } else if (input > minuteRange) {
          let seconds = input / secondRange;
          return Math.floor(seconds / 60) + 'min ' +
            (hideSecondary ? '' : numberFilter(Math.floor(seconds % 60), 0) + 's');
        } else if (input >= secondRange) {
          let val = input / secondRange;
          return numberFilter(val, val >= 100 ? 1 : (val >= 10 ? 2 : 3)) + 's';
        } else {
          let val = input / milliRange;
          return numberFilter(val, val >= 100 ? 1 : (val >= 10 ? 2 : 3)) + 'ms';
        }
      } else {
        return input;
      }
    };
  });

  hawtioPluginLoader.addModule(E2E.pluginName);
}
